/*
 * MediaReferenceCommands：媒体模块的工具文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：MediaReferenceInvalidException、MediaUsageCodes、MediaReferenceCommand。
 * 阅读入口：normalize（规范化媒体引用Commands）；normalizeSource（规范化来源）。
 */

package com.starrainnotes.media.utils;

import com.starrainnotes.media.exception.MediaReferenceInvalidException;
import com.starrainnotes.media.api.constant.MediaUsageCodes;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import java.util.Locale;

/*
 * 媒体引用参数的规范化与校验。
 *
 * sourceModule / sourceType 统一转大写；usageCode 保持小写，
 * 并强制其前缀与 sourceModule 一致，避免把 blog.cover 挂到 TUTORIAL 上。
 * 这里是“浏览器不能伪造内部模块调用参数”的第一道防线。
 */
/**
 * 媒体引用Commands，属于媒体模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class MediaReferenceCommands {

    /**
     * LENGTH。
     * 初始值为 <code>50</code>。
     */
    private static final int MAX_MODULE_LENGTH = 50;
    /**
     * LENGTH。
     * 初始值为 <code>50</code>。
     */
    private static final int MAX_TYPE_LENGTH = 50;
    /**
     * LENGTH。
     * 初始值为 <code>100</code>。
     */
    private static final int MAX_USAGE_LENGTH = 100;

    /**
     * 构造MediaReferenceCommands，接收本类使用的依赖或初始化参数。
     */
    private MediaReferenceCommands() {
    }

    // attach / detach 共用的校验；返回规范化后的命令，调用方不要再用原始入参
    /**
     * 规范化媒体引用Commands。
     * 关键处理步骤调用 <code>command.getMediaAssetId</code>、<code>command.getSourceId</code>、<code>upperSegment</code>、<code>command.getSourceModule</code>。
     * 实现中的显式失败类型包括 <code>MediaReferenceInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(command == null)</code>；<code>(mediaAssetId == null || mediaAssetId &lt;= 0)</code>。
     *
     * @param command command；类型为 <code>MediaReferenceCommand</code>
     * @return 类型为 <code>MediaReferenceCommand</code> 的处理结果
     */
    public static MediaReferenceCommand normalize(MediaReferenceCommand command) {
        if (command == null) {
            throw new MediaReferenceInvalidException("媒体引用参数缺失");
        }
        Long mediaAssetId = command.getMediaAssetId();
        if (mediaAssetId == null || mediaAssetId <= 0) {
            throw new MediaReferenceInvalidException("mediaAssetId 必须为正整数");
        }
        Long sourceId = command.getSourceId();
        if (sourceId == null || sourceId <= 0) {
            throw new MediaReferenceInvalidException("sourceId 必须为正整数");
        }
        String sourceModule = upperSegment(command.getSourceModule(), "sourceModule", MAX_MODULE_LENGTH);
        String sourceType = upperSegment(command.getSourceType(), "sourceType", MAX_TYPE_LENGTH);
        String usageCode = command.getUsageCode() == null ? "" : command.getUsageCode().trim();
        if (usageCode.isEmpty() || usageCode.length() > MAX_USAGE_LENGTH) {
            throw new MediaReferenceInvalidException("usageCode 不能为空且不超过 " + MAX_USAGE_LENGTH + " 个字符");
        }
        if (!MediaUsageCodes.hasValidFormat(usageCode)) {
            throw new MediaReferenceInvalidException("usageCode 必须形如 blog.cover");
        }
        if (!MediaUsageCodes.matchesSourceModule(usageCode, sourceModule)) {
            throw new MediaReferenceInvalidException("usageCode 前缀必须与 sourceModule 一致");
        }
        return new MediaReferenceCommand(mediaAssetId, sourceModule, sourceType, sourceId, usageCode);
    }

    // detachAll 只接受来源三元组，校验规则与 attach 保持一致
    /**
     * 规范化来源。
     * 关键处理步骤调用 <code>upperSegment</code>。
     * 实现中的显式失败类型包括 <code>MediaReferenceInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(sourceId == null || sourceId &lt;= 0)</code>。
     *
     * @param sourceModule 引用或数据来源所属模块；类型为 <code>String</code>
     * @param sourceType 来源对象的业务类型；类型为 <code>String</code>
     * @param sourceId 来源对象的业务标识；类型为 <code>Long</code>
     * @return 类型为 <code>String[]</code> 的处理结果
     */
    public static String[] normalizeSource(String sourceModule, String sourceType, Long sourceId) {
        if (sourceId == null || sourceId <= 0) {
            throw new MediaReferenceInvalidException("sourceId 必须为正整数");
        }
        return new String[]{
                upperSegment(sourceModule, "sourceModule", MAX_MODULE_LENGTH),
                upperSegment(sourceType, "sourceType", MAX_TYPE_LENGTH)};
    }

    /**
     * upperSegment处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>value.trim</code>、<code>trimmed.length</code>、<code>trimmed.toUpperCase</code>。
     * 实现中的显式失败类型包括 <code>MediaReferenceInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(value == null || value.isBlank())</code>；<code>(trimmed.length() &gt; maxLength)</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @param label 标签；类型为 <code>String</code>
     * @param maxLength 最大Length；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String upperSegment(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new MediaReferenceInvalidException(label + " 不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new MediaReferenceInvalidException(label + " 不超过 " + maxLength + " 个字符");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }
}
