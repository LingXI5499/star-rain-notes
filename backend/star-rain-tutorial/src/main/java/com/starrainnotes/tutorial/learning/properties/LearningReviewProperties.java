/*
 * LearningReviewProperties：教程与学习模块的外部配置文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接配置文件和环境变量映射。字段初始值是默认值，实际部署可以通过配置绑定覆盖。
 */

package com.starrainnotes.tutorial.learning.properties;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 学习审核配置属性，属于教程与学习模块。
 * 承接配置文件和环境变量映射。字段初始值是默认值，实际部署可以通过配置绑定覆盖。
 */
@Data
@ConfigurationProperties(prefix = "star-rain.learning.review")
public class LearningReviewProperties {
    /**
     * 基础间隔Days。
     */
    private List<Integer> baseIntervalDays = List.of(1, 3, 7, 14, 30, 60);
}
