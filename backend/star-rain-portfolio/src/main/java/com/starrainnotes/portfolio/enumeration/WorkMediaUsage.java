/*
 * WorkMediaUsage：作品模块的枚举文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 * 主要协作类型：MediaUsageCodes。
 * 阅读入口：usageCode（usage代码处理入口）。
 */

package com.starrainnotes.portfolio.enumeration;

import com.starrainnotes.media.api.constant.MediaUsageCodes;

/**
 * 作品媒体Usage，属于作品模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum WorkMediaUsage {
    /**
     * 封面。
     */
    COVER(MediaUsageCodes.PORTFOLIO_COVER),
    /**
     * 截图。
     */
    SCREENSHOT(MediaUsageCodes.PORTFOLIO_SCREENSHOT),
    /**
     * 音频。
     */
    AUDIO(MediaUsageCodes.PORTFOLIO_AUDIO),
    /**
     * ATTACHMENT。
     */
    ATTACHMENT(MediaUsageCodes.PORTFOLIO_ATTACHMENT);

    /**
     * usage代码。
     */
    private final String usageCode;

    /**
     * 构造WorkMediaUsage，接收本类使用的依赖或初始化参数。
     *
     * @param usageCode usage代码；类型为 <code>String</code>
     */
    WorkMediaUsage(String usageCode) {
        this.usageCode = usageCode;
    }

    /**
     * usage代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String usageCode() {
        return usageCode;
    }
}
