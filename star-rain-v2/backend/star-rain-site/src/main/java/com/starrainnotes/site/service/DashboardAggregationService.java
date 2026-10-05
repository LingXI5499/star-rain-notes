package com.starrainnotes.site.service;

import com.starrainnotes.site.vo.SiteDashboardVO;

/** Aggregates dashboard data from module sources. */
public interface DashboardAggregationService {
    /** Returns available dashboard modules and lists sources that degraded. */
    SiteDashboardVO dashboard();
}
