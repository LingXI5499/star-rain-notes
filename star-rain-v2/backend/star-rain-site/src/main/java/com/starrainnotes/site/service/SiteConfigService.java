package com.starrainnotes.site.service;

import com.starrainnotes.site.api.SitePublicApi;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import com.starrainnotes.site.dto.SiteConfigPatchDTO;
import com.starrainnotes.site.dto.SiteMediaRequestDTO;

/** Updates the single public site configuration and its media references. */
public interface SiteConfigService extends SitePublicApi {
    /** Updates the primary site's text fields. */
    SitePublicConfigVO patch(SiteConfigPatchDTO patch);

    /** Sets a logo or favicon from a required request body. */
    SitePublicConfigVO setMediaRequired(String kind, SiteMediaRequestDTO request);

    /** Sets or removes a logo or favicon and updates media references. */
    SitePublicConfigVO setMedia(String kind, Long assetId);
}
