/*
 * SiteDashboardVO：站点模块的响应数据文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 * 主要协作类型：DashboardModuleData、DashboardRecentItem。
 */

package com.starrainnotes.site.vo;

import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.common.dashboard.dto.DashboardRecentItem;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 站点概览响应对象，属于站点模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@AllArgsConstructor
public class SiteDashboardVO {
    /**
     * modules。
     */
    private Map<String, DashboardModuleData> modules;
    /**
     * recent内容。
     */
    private List<DashboardRecentItem> recentContent;
    /**
     * degradedModules。
     */
    private List<String> degradedModules;
}
