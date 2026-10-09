package com.starrainnotes.english.vocabulary.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/*
 * 站点时间口径。
 *
 * 数据库按 UTC 存 DATETIME(3)（连接串固定 SET time_zone='+00:00'），对外输出统一带偏移量，
 * 前端 new Date(iso) 才不会把 UTC 当成浏览器本地时间。
 * 与教程学习域一致，站点时区固定为 Asia/Shanghai。
 */
public final class VocabularySiteTime {

    private VocabularySiteTime() { }

    public static final ZoneId SITE_ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static String format(LocalDateTime utcValue) {
        return utcValue == null ? null : utcValue.atZone(java.time.ZoneOffset.UTC)
                .withZoneSameInstant(SITE_ZONE)
                .format(ISO_OFFSET);
    }

    /* 站点时区的今天零点，换算回 UTC，用于「今日完成」统计 */
    public static LocalDateTime todayStartUtc() {
        return java.time.LocalDate.now(SITE_ZONE).atStartOfDay(SITE_ZONE)
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .toLocalDateTime();
    }

    /* 站点时区的今日序数，MIXED 方向按天稳定取向 */
    public static long todayEpochDay() {
        return java.time.LocalDate.now(SITE_ZONE).toEpochDay();
    }
}
