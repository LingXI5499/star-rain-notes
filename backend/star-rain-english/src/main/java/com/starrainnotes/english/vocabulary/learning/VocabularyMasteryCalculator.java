/*
 * VocabularyMasteryCalculator：英语学习模块的模块支持文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：ModeMemory、VocabularySiteTime。
 * 阅读入口：updatedEma（更新Ema处理入口）；score（score处理入口）；graduated（graduated处理入口）；rank（rank处理入口）。
 */

package com.starrainnotes.english.vocabulary.learning;

import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels.ModeMemory;
import com.starrainnotes.english.vocabulary.utils.VocabularySiteTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** Pure product scoring model; it estimates self-rated recall, not language proficiency. */
public final class VocabularyMasteryCalculator {
    /**
     * DIRECTIONS。
     */
    public static final List<String> DIRECTIONS = List.of("EN_TO_ZH", "ZH_TO_EN", "AUDIO_TO_BOTH");
    /**
     * 构造VocabularyMasteryCalculator，接收本类使用的依赖或初始化参数。
     */
    private VocabularyMasteryCalculator() { }

    /**
     * 更新Ema处理入口。
     * 关键处理步骤调用 <code>Math.round</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param previousCount 上一项数量；类型为 <code>int</code>
     * @param previousScore 上一项Score；类型为 <code>double</code>
     * @param rating 评分；类型为 <code>String</code>
     * @return 类型为 <code>double</code> 的处理结果
     */
    public static double updatedEma(int previousCount, double previousScore, String rating) {
        int score = switch (rating) {
            case "FORGOT" -> 0;
            case "UNCERTAIN" -> 60;
            case "KNOW" -> 100;
            default -> throw new IllegalArgumentException("Unknown rating");
        };
        return previousCount == 0 ? score : Math.round((0.4 * previousScore + 0.6 * score) * 10000) / 10000.0;
    }

    /**
     * score处理入口。
     * 关键处理步骤调用 <code>modes.stream</code>、<code>direction.equals</code>、<code>item.getDirection</code>、<code>mode.getEmaScore</code>。
     *
     * @param modes modes；类型为 <code>List&lt;ModeMemory&gt;</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    public static int score(List<ModeMemory> modes) {
        double sum = 0;
        for (String direction : DIRECTIONS) {
            ModeMemory mode = modes.stream().filter(item -> direction.equals(item.getDirection())).findFirst().orElse(null);
            if (mode != null) {
                sum += mode.getEmaScore() * Math.min(1, mode.getRatingCount() / 3.0);
            }
        }
        return (int) Math.max(0, Math.min(100, Math.round(sum / 3)));
    }

    /**
     * graduated处理入口。
     * 关键处理步骤调用 <code>score</code>、<code>siteDay</code>、<code>modes.stream</code>、<code>direction.equals</code>。
     *
     * @param modes modes；类型为 <code>List&lt;ModeMemory&gt;</code>
     * @param firstRatedAt 首次Rated时间；类型为 <code>LocalDateTime</code>
     * @param now now；类型为 <code>LocalDateTime</code>
     * @param totalDays 总数Days；类型为 <code>int</code>
     * @param recentDays recentDays；类型为 <code>int</code>
     * @return 本方法条件是否成立的判断结果
     */
    public static boolean graduated(List<ModeMemory> modes, LocalDateTime firstRatedAt,
                                    LocalDateTime now, int totalDays, int recentDays) {
        if (firstRatedAt == null || score(modes) < 80 || totalDays < 3 || recentDays < 2
                || siteDay(firstRatedAt).plusDays(7).isAfter(siteDay(now))) {
            return false;
        }
        for (String direction : DIRECTIONS) {
            ModeMemory mode = modes.stream().filter(item -> direction.equals(item.getDirection())).findFirst().orElse(null);
            if (mode == null || mode.getRatingCount() < 3 || !"KNOW".equals(mode.getLastRating())
                    || ("AUDIO_TO_BOTH".equals(direction) && !mode.isAudioVerified())) {
                return false;
            }
        }
        return true;
    }

    /**
     * rank处理入口。
     *
     * @param score score；类型为 <code>int</code>
     * @param graduated graduated；类型为 <code>boolean</code>
     * @param rated rated；类型为 <code>boolean</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String rank(int score, boolean graduated, boolean rated) {
        if (!rated) { return null; }
        if (graduated) { return "MASTERED"; }
        return score < 25 ? "BEGINNER" : score < 50 ? "LEARNER" : "SKILLED";
    }

    /**
     * 站点日处理入口。
     * 关键处理步骤调用 <code>utc.atOffset</code>。
     *
     * @param utc utc；类型为 <code>LocalDateTime</code>
     * @return 类型为 <code>LocalDate</code> 的处理结果
     */
    public static LocalDate siteDay(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(VocabularySiteTime.SITE_ZONE).toLocalDate();
    }
}
