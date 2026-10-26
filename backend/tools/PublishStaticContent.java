import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.context.AccountPrincipal;
import com.starrainnotes.blog.service.BlogPublishService;
import com.starrainnotes.boot.StarRainV2Application;
import com.starrainnotes.search.service.SearchRebuildService;
import com.starrainnotes.seo.service.SeoRebuildService;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.AbstractApplicationEventMulticaster;
import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Local maintenance entry point; compiled separately, never shipped in the application JAR. */
public final class PublishStaticContent {
    private PublishStaticContent() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected pending plan and private configuration paths");
        }
        Path planFile = Path.of(args[0]).toRealPath();
        Path config = Path.of(args[1]).toRealPath();
        Path root = Path.of("").toRealPath().getParent(); // Run from the active backend directory.
        if (!planFile.startsWith(root.resolve("history")) || !config.startsWith(root)) {
            throw new IllegalArgumentException("Maintenance inputs must stay inside the project");
        }
        ObjectMapper json = new ObjectMapper();
        JsonNode plan = json.readTree(Files.readString(planFile));
        String[] options = {
            "--spring.config.additional-location=" + config.toUri(),
            "--server.address=127.0.0.1", "--server.port=0", "--management.server.port=0",
            "--STAR_RAIN_BOOTSTRAP_ADMIN_ENABLED=false", "--STAR_RAIN_SMTP_ENABLED=false",
            "--STAR_RAIN_INDEXNOW_ENABLED=false", "--STAR_RAIN_MEDIA_ORPHAN_CLEANUP=false",
            "--spring.task.scheduling.enabled=false", "--logging.level.root=WARN"
        };
        try (ConfigurableApplicationContext context = SpringApplication.run(StarRainV2Application.class, options)) {
            // Batch maintenance rebuilds these projections once in the same
            // transaction. Only this temporary process coalesces their listeners.
            context.getBean("applicationEventMulticaster", AbstractApplicationEventMulticaster.class)
                .removeApplicationListeners(listener -> listener instanceof ApplicationListenerMethodAdapter adapter
                    && (adapter.getListenerId().contains("SearchEventConsumer")
                        || adapter.getListenerId().contains("SeoContentEventConsumer")));
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            try (Connection connection = jdbc.getDataSource().getConnection()) {
                String url = connection.getMetaData().getURL();
                if (!url.matches("jdbc:mysql://(localhost|127\\.0\\.0\\.1)(:\\d+)?/[\\w-]+(\\?.*)?")
                        || !connection.getCatalog().equals(plan.path("targetDatabase").asText())) {
                    throw new IllegalArgumentException("The local database does not match the reviewed plan");
                }
            }
            long ownerId = plan.path("ownerAccountId").asLong();
            Map<String, Object> owner = jdbc.queryForMap(
                "SELECT id, username, auth_version FROM sr_account WHERE id=? AND status='ACTIVE'", ownerId);
            Set<String> roles = new HashSet<>(jdbc.queryForList(
                "SELECT r.code FROM sr_role r JOIN sr_account_role ar ON ar.role_id=r.id WHERE ar.account_id=?", String.class, ownerId));
            if (!roles.contains("SUPER_ADMIN")) {
                throw new IllegalStateException("The designated existing account is not the owner");
            }
            Set<String> permissions = new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT p.code FROM sr_permission p JOIN sr_role_permission rp ON rp.permission_id=p.id "
                    + "JOIN sr_account_role ar ON ar.role_id=rp.role_id WHERE ar.account_id=? AND p.status='ENABLED'",
                String.class, ownerId));
            AccountPrincipal principal = new AccountPrincipal(ownerId, owner.get("username").toString(),
                ((Number) owner.get("auth_version")).intValue(), roles, permissions);
            Set<SimpleGrantedAuthority> authorities = new HashSet<>();
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
            try {
                TransactionTemplate transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
                Map<String, Object> result = transaction.execute(status -> {
                    TutorialPublicationService tutorials = context.getBean(TutorialPublicationService.class);
                    for (JsonNode id : plan.path("publishTutorialIds")) {
                        tutorials.publish(id.asLong());
                    }
                    BlogPublishService blogs = context.getBean(BlogPublishService.class);
                    for (JsonNode id : plan.path("publishBlogIds")) {
                        Integer published = jdbc.queryForObject("SELECT COUNT(*) FROM sr_blog_post WHERE id=? AND status='PUBLISHED'", Integer.class, id.asLong());
                        if (published == 0) {
                            blogs.publish(id.asLong());
                        }
                    }
                    Map<String, Object> counts = new LinkedHashMap<>();
                    counts.put("searchDocuments", context.getBean(SearchRebuildService.class).rebuild(null));
                    counts.put("seoPages", context.getBean(SeoRebuildService.class).rebuild(null));
                    return counts;
                });
                Files.writeString(planFile.resolveSibling("publication-result.json"), json.writeValueAsString(result));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
