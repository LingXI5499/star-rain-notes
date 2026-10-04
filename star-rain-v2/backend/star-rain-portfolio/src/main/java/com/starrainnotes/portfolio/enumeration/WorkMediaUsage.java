package com.starrainnotes.portfolio.enumeration;

import com.starrainnotes.media.constant.MediaUsageCodes;

public enum WorkMediaUsage {
    COVER(MediaUsageCodes.PORTFOLIO_COVER),
    SCREENSHOT(MediaUsageCodes.PORTFOLIO_SCREENSHOT),
    AUDIO(MediaUsageCodes.PORTFOLIO_AUDIO),
    ATTACHMENT(MediaUsageCodes.PORTFOLIO_ATTACHMENT);

    private final String usageCode;

    WorkMediaUsage(String usageCode) {
        this.usageCode = usageCode;
    }

    public String usageCode() {
        return usageCode;
    }
}
