package com.starrainnotes.site.service;

import com.starrainnotes.site.vo.SiteHomeVO;

/** Aggregates enabled homepage sections into the public view. */
public interface HomeAggregationService {
    /** Returns enabled homepage sections, degrading failed sources independently. */
    SiteHomeVO home();
}
