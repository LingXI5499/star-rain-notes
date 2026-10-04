package com.starrainnotes.seo.mapper;

import com.starrainnotes.seo.snapshot.SeoPageSnapshot;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SeoPageMapper {
    int upsert(SeoPageSnapshot snapshot);
    int remove(@Param("routePath") String routePath);
    SeoPageSnapshot active(@Param("routePath") String routePath);
    SeoPageSnapshot activeMeta(@Param("routePath") String routePath);
    List<SeoPageSnapshot> activePages();
    List<String> activePaths();
    List<String> activePathsByPrefix(@Param("prefix") String prefix);
    long activeCount();
}
