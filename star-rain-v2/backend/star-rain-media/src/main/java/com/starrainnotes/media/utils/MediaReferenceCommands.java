package com.starrainnotes.media.utils;

import com.starrainnotes.media.exception.MediaReferenceInvalidException;
import com.starrainnotes.media.constant.MediaUsageCodes;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import java.util.Locale;

/*
 * 媒体引用参数的规范化与校验。
 *
 * sourceModule / sourceType 统一转大写；usageCode 保持小写，
 * 并强制其前缀与 sourceModule 一致，避免把 blog.cover 挂到 TUTORIAL 上。
 * 这里是“浏览器不能伪造内部模块调用参数”的第一道防线。
 */
public final class MediaReferenceCommands {

    private static final int MAX_MODULE_LENGTH = 50;
    private static final int MAX_TYPE_LENGTH = 50;
    private static final int MAX_USAGE_LENGTH = 100;

    private MediaReferenceCommands() {
    }

    // attach / detach 共用的校验；返回规范化后的命令，调用方不要再用原始入参
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
    public static String[] normalizeSource(String sourceModule, String sourceType, Long sourceId) {
        if (sourceId == null || sourceId <= 0) {
            throw new MediaReferenceInvalidException("sourceId 必须为正整数");
        }
        return new String[]{
                upperSegment(sourceModule, "sourceModule", MAX_MODULE_LENGTH),
                upperSegment(sourceType, "sourceType", MAX_TYPE_LENGTH)};
    }

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
