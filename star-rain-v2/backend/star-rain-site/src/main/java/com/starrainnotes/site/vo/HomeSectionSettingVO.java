package com.starrainnotes.site.vo;

import com.starrainnotes.site.entity.HomeSectionEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 后台「首页区块设置」的视图对象（管理端列表 / 排序 / 单区块修改的响应）。
 *
 * 为什么需要它：`HomeSectionAdminController` 原先直接返回 `HomeSectionEntity`，
 * 实体上每加一列都会**自动出现在管理端响应里**，前端与数据库表结构从此隐式耦合。
 * 现在响应类型是 VO，契约由这个类显式决定。
 *
 * 命名说明：`site/vo/HomeSectionVO` 已经被**公开首页**的区块载荷占用
 * （code/title/data/status/layout），所以后台这个叫 `HomeSectionSettingVO`，
 * 两个名字分别对应「公开渲染结果」与「后台可配置项」，不再混用。
 *
 * 字段名与实体逐字相同，因此管理端 JSON 契约不变，前端不需要改。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeSectionSettingVO {
    private Long id;
    private String sectionCode;
    private String displayName;
    private Boolean enabled;
    private Integer sortOrder;
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static HomeSectionSettingVO from(HomeSectionEntity entity) {
        if (entity == null) return null;
        return HomeSectionSettingVO.builder()
                .id(entity.getId())
                .sectionCode(entity.getSectionCode())
                .displayName(entity.getDisplayName())
                .enabled(entity.getEnabled())
                .sortOrder(entity.getSortOrder())
                .configJson(entity.getConfigJson())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
