/*
 * LearningPageQueryDTO：教程与学习模块的请求与传输数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 * 主要协作类型：LearningInvalidRequestException。
 * 阅读入口：validate（校验学习分页查询参数）；getOffset（读取偏移）；getFromTime（读取来源时间）；getUntilTime（读取Until时间）。
 */

package com.starrainnotes.tutorial.learning.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.starrainnotes.tutorial.learning.exception.LearningInvalidRequestException;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/** Account queries; dates use the site's Asia/Shanghai calendar and UTC storage. */
@Data
public class LearningPageQueryDTO {
    /**
     * 分页页码。
     * 校验规则 <code>@Min(1)</code>。
     * 初始值为 <code>1</code>。
     */
    @Min(1) private int page = 1;
    /**
     * 单页返回条目数。
     * 校验规则 <code>@Min(1)</code>。
     * 校验规则 <code>@Max(100)</code>。
     * 初始值为 <code>20</code>。
     */
    @Min(1) @Max(100) private int pageSize = 20;
    /**
     * 教程标识。
     * 校验规则 <code>@Positive</code>。
     */
    @Positive private Long tutorialId;
    /**
     * 关键词。
     * 校验规则 <code>@Size(max = 100)</code>。
     */
    @Size(max = 100) private String keyword;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 会话类型。
     */
    private String sessionType;
    /**
     * needsRevalidation。
     */
    private Boolean needsRevalidation;
    /**
     * 来源日期。
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate fromDate;
    /**
     * 目标日期。
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate toDate;

    /**
     * 校验学习分页查询参数。
     * 主要协作调用：<code>keyword.length</code>、<code>status.isBlank</code>、<code>sessionType.isBlank</code>、<code>fromDate.isAfter</code>、<code>fromDate.getYear</code>。
     * 实现中的显式失败类型包括 <code>LearningInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param statuses statuses；类型为 <code>Set&lt;String&gt;</code>
     */
    public void validate(Set<String> statuses) {
        if (page < 1 || pageSize < 1 || pageSize > 100 || (long) (page - 1) * pageSize > Integer.MAX_VALUE
                || tutorialId != null && tutorialId <= 0 || keyword != null && keyword.length() > 100
                || status != null && !status.isBlank() && !statuses.contains(status)
                || sessionType != null && !sessionType.isBlank() && !Set.of("INITIAL_STUDY", "REVIEW", "STABLE_AUDIT").contains(sessionType)
                || fromDate != null && toDate != null && fromDate.isAfter(toDate)
                || fromDate != null && (fromDate.getYear() < 1970 || fromDate.getYear() > 9998)
                || toDate != null && (toDate.getYear() < 1970 || toDate.getYear() > 9998)) {
            throw new LearningInvalidRequestException("查询条件或分页参数无效");
        }
    }
    /**
     * 读取偏移。
     * @return 类型为 <code>int</code> 的处理结果
     */
    @JsonIgnore public int getOffset() { return (page - 1) * pageSize; }
    /**
     * 读取来源时间。
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
    @JsonIgnore public LocalDateTime getFromTime() { return utc(fromDate); }
    /**
     * 读取Until时间。
     * 主要协作调用：<code>toDate.plusDays</code>。
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
    @JsonIgnore public LocalDateTime getUntilTime() { return utc(toDate == null ? null : toDate.plusDays(1)); }
    /**
     * utc处理入口。
     *
     * @param date 日期；类型为 <code>LocalDate</code>
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
    private LocalDateTime utc(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneId.of("Asia/Shanghai")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
