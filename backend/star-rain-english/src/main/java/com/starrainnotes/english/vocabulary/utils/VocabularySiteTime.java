/*
 * VocabularySiteTime：英语学习模块的工具文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 阅读入口：format（格式化词汇站点时间）；todayStartUtc（today开始Utc处理入口）；todayEpochDay（todayEpoch日处理入口）。
 */

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
/**
 * 词汇站点时间，属于英语学习模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class VocabularySiteTime {

    /**
     * 构造VocabularySiteTime，接收本类使用的依赖或初始化参数。
     */
    private VocabularySiteTime() { }

    /**
     * ZONE。
     */
    public static final ZoneId SITE_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 偏移。
     */
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * 格式化词汇站点时间。
     * 关键处理步骤调用 <code>utcValue.atZone</code>。
     *
     * @param utcValue utc值；类型为 <code>LocalDateTime</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String format(LocalDateTime utcValue) {
        return utcValue == null ? null : utcValue.atZone(java.time.ZoneOffset.UTC)
                .withZoneSameInstant(SITE_ZONE)
                .format(ISO_OFFSET);
    }

    /* 站点时区的今天零点，换算回 UTC，用于「今日完成」统计 */
    /**
     * today开始Utc处理入口。
     * 关键处理步骤调用 <code>java.time.LocalDate.now</code>。
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
    public static LocalDateTime todayStartUtc() {
        return java.time.LocalDate.now(SITE_ZONE).atStartOfDay(SITE_ZONE)
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .toLocalDateTime();
    }

    /* 站点时区的今日序数，MIXED 方向按天稳定取向 */
    /**
     * todayEpoch日处理入口。
     * 关键处理步骤调用 <code>java.time.LocalDate.now</code>。
     * @return 类型为 <code>long</code> 的处理结果
     */
    public static long todayEpochDay() {
        return java.time.LocalDate.now(SITE_ZONE).toEpochDay();
    }
}
