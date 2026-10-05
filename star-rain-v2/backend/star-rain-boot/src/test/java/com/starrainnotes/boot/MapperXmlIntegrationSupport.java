package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

/*
 * MyBatis XML 的真实数据库验证骨架（类名不以 Test 结尾，surefire 不会直接执行它）。
 *
 * 为什么需要它：`mvn test` 里全是 Mockito 单元测试，Mapper 被 mock 掉之后，
 * XML 里的语句从没被 MyBatis 解析过 —— 语句 id、参数名、`<if>` 分支、列清单、
 * resultType 映射全都没有验证。把 `@Select` 注解搬进 XML 这类重构因此可能是
 * 「编译通过、运行期才报 Invalid bound statement」。
 *
 * 分两层，对应两类事实：
 *   1. **解析层（不需要数据库）**：按生产同样的 `mapper-locations` 约定加载全部 XML，
 *      检查每个 namespace 都能绑定到接口、每个语句 id 都能对应上接口方法、
 *      反过来接口方法也都有语句。这些是纯静态事实，没有 MySQL 也必须在 CI 上跑。
 *   2. **连库层（连不上就 skip）**：用 UnpooledDataSource 连真实库跑代表性读写，
 *      验证参数绑定与结果映射，不 commit、结束时 rollback，不改动开发库数据。
 *
 * 刻意**不启动 Spring 上下文**：没有邮件、定时任务、Flyway 这些副作用，秒级完成。
 *
 * 数据库连接按这个顺序解析，都不满足就连库层 skip（不 fail）：
 *   1. 环境变量 STAR_RAIN_DB_URL / STAR_RAIN_DB_USERNAME / STAR_RAIN_DB_PASSWORD
 *   2. star-rain-v2/backend/application-local-secret.yml（本机开发配置，不入库）
 */
abstract class MapperXmlIntegrationSupport {

    private static final Pattern JDBC = Pattern.compile(
            "^jdbc:mysql://(?<host>[^:/?]+):(?<port>\\d+)/(?<db>[^?]+)");
    private static final Pattern NAMESPACE = Pattern.compile("namespace=\"([^\"]+)\"");

    private static Configuration parsedConfiguration;
    private static SqlSessionFactory sqlSessionFactory;
    private static DbConfig resolvedDb;
    private static boolean dbResolved;
    private static String skipReason;

    /** 解析层配置：只加载 XML，不带数据源。 */
    protected static synchronized Configuration configuration() {
        if (parsedConfiguration == null) {
            parsedConfiguration = parseInto(baseConfiguration());
        }
        return parsedConfiguration;
    }

    /**
     * 连库层：连不上时 Assumptions 让用例变 skipped 而不是失败，
     * 这样没装 MySQL 的构建仍是绿的，装了 MySQL 的机器上会真正执行 SQL。
     * 调用方负责 rollback + close。
     */
    protected static SqlSession openSession() {
        assumeTrue(dbConfig() != null, "跳过需要数据库的 XML 验证：" + skipReason);
        return factory().openSession();
    }

    /** 后端根目录下所有模块的 Mapper XML，按生产 mapper-locations 的相对路径。 */
    protected static List<String> xmlResources() {
        return discoverMapperXmlResources();
    }

    protected static String namespaceOf(String resource) {
        Matcher matcher = NAMESPACE.matcher(readResource(resource));
        assertNotNull(matcher.find() ? matcher.group(1) : null, "XML 缺少 namespace：" + resource);
        return matcher.group(1);
    }

    protected static String readResource(String resource) {
        try (InputStream in = Resources.getResourceAsStream(resource)) {
            assertNotNull(in, "classpath 上找不到 Mapper XML：" + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("读取 " + resource + " 失败", exception);
        }
    }

    private static Configuration baseConfiguration() {
        Configuration configuration = new Configuration();
        // 与 application.yml 的 mybatis-plus.configuration.map-underscore-to-camel-case 对齐
        configuration.setMapUnderscoreToCamelCase(true);
        return configuration;
    }

    private static Configuration parseInto(Configuration configuration) {
        for (String resource : discoverMapperXmlResources()) {
            try (InputStream in = Resources.getResourceAsStream(resource)) {
                assertNotNull(in, "classpath 上找不到 Mapper XML：" + resource);
                new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
            } catch (IOException exception) {
                throw new IllegalStateException("解析 " + resource + " 失败", exception);
            }
        }
        return configuration;
    }

    private static synchronized SqlSessionFactory factory() {
        if (sqlSessionFactory != null) return sqlSessionFactory;
        DbConfig config = dbConfig();
        assumeTrue(config != null, "跳过需要数据库的 XML 验证：" + skipReason);
        Configuration configuration = baseConfiguration();
        configuration.setEnvironment(new Environment("integration",
                new JdbcTransactionFactory(),
                new UnpooledDataSource(config.driver, config.url, config.username, config.password)));
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(parseInto(configuration));
        return sqlSessionFactory;
    }

    private static List<String> discoverMapperXmlResources() {
        Path root = backendRoot();
        List<String> resources = new ArrayList<>();
        try (Stream<Path> modules = Files.list(root)) {
            for (Path module : modules.filter(Files::isDirectory).toList()) {
                Path mapperDir = module.resolve("src/main/resources/mapper");
                if (!Files.isDirectory(mapperDir)) continue;
                Path resourcesRoot = module.resolve("src/main/resources");
                try (Stream<Path> files = Files.walk(mapperDir)) {
                    files.filter(path -> path.toString().endsWith(".xml"))
                            .map(path -> resourcesRoot.relativize(path).toString().replace('\\', '/'))
                            .sorted()
                            .forEach(resources::add);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("扫描 Mapper XML 失败", exception);
        }
        return resources;
    }

    private static synchronized DbConfig dbConfig() {
        if (dbResolved) return resolvedDb;
        dbResolved = true;

        String url = System.getenv("STAR_RAIN_DB_URL");
        if (url != null && !url.isBlank()) {
            resolvedDb = new DbConfig(url, System.getenv("STAR_RAIN_DB_USERNAME"),
                    System.getenv("STAR_RAIN_DB_PASSWORD"), "com.mysql.cj.jdbc.Driver");
            return resolvedDb;
        }
        Path secret = backendRoot().resolve("application-local-secret.yml");
        if (!Files.isRegularFile(secret)) {
            skipReason = "既没有 STAR_RAIN_DB_URL，也没有 " + secret;
            return null;
        }
        try {
            String text = Files.readString(secret, StandardCharsets.UTF_8);
            String localUrl = value(text, "STAR_RAIN_DB_URL");
            if (localUrl == null) {
                skipReason = secret + " 里没有 STAR_RAIN_DB_URL";
                return null;
            }
            DbConfig config = new DbConfig(localUrl, value(text, "STAR_RAIN_DB_USERNAME"),
                    value(text, "STAR_RAIN_DB_PASSWORD"), "com.mysql.cj.jdbc.Driver");
            if (!reachable(config)) {
                skipReason = "连不上 " + localUrl;
                return null;
            }
            resolvedDb = config;
            return resolvedDb;
        } catch (IOException exception) {
            skipReason = "读取 " + secret + " 失败：" + exception.getMessage();
            return null;
        }
    }

    private static String value(String yaml, String key) {
        Matcher matcher = Pattern.compile("(?m)^" + Pattern.quote(key) + ":\\s*(.+)$").matcher(yaml);
        if (!matcher.find()) return null;
        return matcher.group(1).trim().replaceAll("^[\"']|[\"']$", "");
    }

    private static boolean reachable(DbConfig config) {
        Matcher matcher = JDBC.matcher(config.url);
        if (!matcher.find()) return false;
        String target = "jdbc:mysql://" + matcher.group("host") + ":" + matcher.group("port")
                + "/" + matcher.group("db") + "?connectTimeout=2000&socketTimeout=4000";
        try (Connection ignored = DriverManager.getConnection(target, config.username, config.password)) {
            return true;
        } catch (Exception unreachable) {
            return false;
        }
    }

    private static Path backendRoot() {
        Path current = Path.of("").toAbsolutePath();
        // 从 star-rain-boot 跑测试时工作目录就是模块目录，向上一级是 backend
        if (current.getFileName() != null && current.getFileName().toString().startsWith("star-rain-")) {
            return current.getParent();
        }
        return current;
    }

    /* 项目约定禁 record，这里用普通 POJO 保持一致。 */
    private static final class DbConfig {
        private final String url;
        private final String username;
        private final String password;
        private final String driver;

        private DbConfig(String url, String username, String password, String driver) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.driver = driver;
        }
    }
}
