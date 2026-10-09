package com.starrainnotes.portfolio.mapper;
import com.starrainnotes.portfolio.entity.*;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface WorkContentMapper {
 List<WorkTaxonomyEntity> categories();
 List<WorkTaxonomyEntity> formats();
 List<WorkTaxonomyEntity> tags();
 List<WorkTaxonomyEntity> workTags(@Param("workId") Long workId);
 List<WorkTemplateEntity> templates();
 WorkTemplateEntity template(@Param("id") Long id);
 Long lockFeatured();
 long featuredCount(@Param("excludeId") Long excludeId);
 void clearTags(@Param("workId") Long workId);
 void addTag(@Param("workId") Long workId,@Param("tagId") Long tagId);
 List<WorkSectionEntity> sections(@Param("workId") Long workId);
 WorkSectionEntity section(@Param("id") Long id);
 void insertSection(WorkSectionEntity row);
 void updateSection(WorkSectionEntity row);
 void orderSection(@Param("id") Long id,@Param("sortOrder") int sortOrder);
 void deleteSection(@Param("id") Long id);
 List<WorkSectionMediaEntity> sectionMedia(@Param("sectionId") Long sectionId);
 void insertSectionMedia(WorkSectionMediaEntity row);
 void clearSectionMedia(@Param("sectionId") Long sectionId);
}
