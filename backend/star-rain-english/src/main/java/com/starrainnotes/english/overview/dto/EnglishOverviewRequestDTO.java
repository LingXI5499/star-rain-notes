package com.starrainnotes.english.overview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 英语总览更新请求体。
 *
 * 字段名就是前端 JSON 字段名，改类名/包名都无所谓，改字段名会静默破坏前端。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnglishOverviewRequestDTO {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 500)
    private String subtitle;

    @Size(max = 10000)
    private String introduction;

    @Size(max = 200000)
    private String roadmapMarkdown;
}
