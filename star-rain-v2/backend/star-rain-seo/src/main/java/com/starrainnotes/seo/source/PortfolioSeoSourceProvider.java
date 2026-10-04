package com.starrainnotes.seo.source;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.exception.WorkNotFoundException;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import com.starrainnotes.portfolio.vo.WorkVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioSeoSourceProvider implements SeoSourceProvider {
    private final PortfolioWorkService works;

    @Override
    public boolean supports(String path) { return path != null && path.matches("/portfolio/[a-zA-Z0-9_-]{1,120}"); }

    @Override
    public Optional<SeoSourceDocument> loadByRoute(String path) {
        if (!supports(path)) return Optional.empty();
        try { return Optional.of(document(works.publicWork(path.substring("/portfolio/".length())))); }
        catch (WorkNotFoundException absent) { return Optional.empty(); }
    }

    @Override
    public List<SeoSourceDocument> listPublished() {
        List<SeoSourceDocument> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            PageResult<WorkVO> rows = works.publicWorks(page, 100, null);
            rows.getItems().forEach(item -> result.add(document(works.publicWorkById(Long.valueOf(item.getId())))));
            if ((long) page * 100 >= rows.getTotal()) break;
        }
        return result;
    }

    private SeoSourceDocument document(WorkVO work) {
        return SeoSourceDocument.builder().routePath("/portfolio/" + work.getSlug())
            .contentType("PORTFOLIO").contentId(Long.valueOf(work.getId()))
            .title(work.getTitle()).summary(work.getSummary()).bodyMarkdown(work.getBodyMarkdown())
            .updatedAt(work.getUpdatedAt()).build();
    }
}
