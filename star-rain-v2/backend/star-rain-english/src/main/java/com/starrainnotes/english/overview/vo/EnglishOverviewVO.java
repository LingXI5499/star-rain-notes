package com.starrainnotes.english.overview.vo;

import com.starrainnotes.english.overview.entity.EnglishOverviewEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 英语总览的响应视图。
 *
 * 字段名就是前端 JSON 字段名，改类名/包名都无所谓，改字段名会静默破坏前端。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnglishOverviewVO {

    private String title;
    private String subtitle;
    private String introduction;
    private String currentStage;
    private String roadmapMarkdown;

    public static EnglishOverviewVO from(EnglishOverviewEntity entity) {
        return EnglishOverviewVO.builder()
                .title(entity.getTitle())
                .subtitle(entity.getSubtitle())
                .introduction(entity.getIntroduction())
                .currentStage(entity.getCurrentStage())
                .roadmapMarkdown(entity.getRoadmapMarkdown())
                .build();
    }
}
