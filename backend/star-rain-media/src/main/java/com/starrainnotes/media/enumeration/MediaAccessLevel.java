/*
 * MediaAccessLevel：媒体模块的枚举文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 * 主要协作类型：MediaAccessLevelInvalidException。
 * 阅读入口：parse（解析媒体访问Level）；of（所属处理入口）。
 */

package com.starrainnotes.media.enumeration;

import com.starrainnotes.media.exception.MediaAccessLevelInvalidException;
import java.util.Locale;

/*
 * 媒体访问级别。
 *
 * PUBLIC    —— 允许匿名读取，用于前台公开内容里的图片、封面等
 * PROTECTED —— 必须经过认证且具备 media:read，用于后台素材与简历等
 */
/**
 * 媒体访问Level，属于媒体模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum MediaAccessLevel {
    /**
     * 公开。
     */
    PUBLIC,
    /**
     * 受保护。
     */
    PROTECTED;

    // 严格解析：非法值直接抛 MEDIA_ACCESS_LEVEL_INVALID，用于写操作
    /**
     * 解析媒体访问Level。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>MediaAccessLevel.valueOf</code>、<code>value.trim</code>。
     * 实现中的显式失败类型包括 <code>MediaAccessLevelInvalidException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>MediaAccessLevel</code> 的处理结果
     */
    public static MediaAccessLevel parse(String value) {
        if (value == null || value.isBlank()) {
            throw new MediaAccessLevelInvalidException();
        }
        try {
            return MediaAccessLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new MediaAccessLevelInvalidException();
        }
    }

    // 宽松解析：用于查询筛选条件，非法值返回 null
    /**
     * 所属处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>MediaAccessLevel.valueOf</code>、<code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>MediaAccessLevel</code> 的处理结果
     */
    public static MediaAccessLevel of(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MediaAccessLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
