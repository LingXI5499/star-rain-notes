/*
 * SiteHomeVO：站点模块的响应数据文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 * 主要协作类型：SitePublicConfigVO。
 */

package com.starrainnotes.site.vo;

import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 站点首页响应对象，属于站点模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@AllArgsConstructor
public class SiteHomeVO {
    /**
     * 配置。
     */
    private SitePublicConfigVO config;
    /**
     * sections。
     */
    private List<HomeSectionVO> sections;
}
