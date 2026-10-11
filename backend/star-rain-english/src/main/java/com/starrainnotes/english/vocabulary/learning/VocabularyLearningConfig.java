/*
 * VocabularyLearningConfig：英语学习模块的模块支持文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 阅读入口：vocabularyLearningClock（词汇学习Clock处理入口）。
 */

package com.starrainnotes.english.vocabulary.learning;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 词汇学习配置，属于英语学习模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Configuration
public class VocabularyLearningConfig {
    /**
     * 词汇学习Clock处理入口。
     * 关键处理步骤调用 <code>Clock.systemUTC</code>。
     * @return 类型为 <code>Clock</code> 的处理结果
     */
    @Bean("vocabularyLearningClock")
    public Clock vocabularyLearningClock() { return Clock.systemUTC(); }
}
