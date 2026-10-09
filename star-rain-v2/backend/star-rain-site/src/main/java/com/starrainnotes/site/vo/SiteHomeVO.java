package com.starrainnotes.site.vo;

import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SiteHomeVO {
    private SitePublicConfigVO config;
    private List<HomeSectionVO> sections;
}
