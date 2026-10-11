package com.starrainnotes.media.enumeration;

import com.starrainnotes.media.exception.MediaAccessLevelInvalidException;
import java.util.Locale;

/*
 * 媒体访问级别。
 *
 * PUBLIC    —— 允许匿名读取，用于前台公开内容里的图片、封面等
 * PROTECTED —— 必须经过认证且具备 media:read，用于后台素材与简历等
 */
public enum MediaAccessLevel {
    PUBLIC,
    PROTECTED;

    // 严格解析：非法值直接抛 MEDIA_ACCESS_LEVEL_INVALID，用于写操作
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
