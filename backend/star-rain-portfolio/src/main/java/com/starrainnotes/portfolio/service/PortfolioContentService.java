package com.starrainnotes.portfolio.service;
import com.starrainnotes.portfolio.dto.WorkSectionDTO;
import com.starrainnotes.portfolio.vo.*;
import java.util.List;
import java.util.Map;
public interface PortfolioContentService {
    Map<String, List<WorkTaxonomyVO>> taxonomy();
    List<WorkTemplateVO> templates();
    List<WorkSectionVO> sections(Long workId, boolean publicOnly);
    List<WorkTaxonomyVO> tags(Long workId);
    void setTags(Long workId, List<Long> ids);
    void copyTemplate(Long workId, Long templateId);
    WorkSectionVO create(Long workId, WorkSectionDTO request);
    WorkSectionVO update(Long workId, Long sectionId, WorkSectionDTO request);
    void delete(Long workId, Long sectionId);
    void reorder(Long workId, List<Long> ids);
    void deleteAll(Long workId);
    void validatePublication(Long workId);
    String searchableText(Long workId, String legacy);
    String seoMarkdown(Long workId, String legacy);
    void validateTaxonomy(Long categoryId, Long formatId);
    void validateFeatured(Long workId, boolean featured);
}
