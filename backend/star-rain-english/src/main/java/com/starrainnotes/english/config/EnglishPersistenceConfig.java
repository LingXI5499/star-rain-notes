/*
 * EnglishPersistenceConfig：英语学习模块的装配配置文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.english.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/*
 * English 的业务子域各自持有 mapper 包，因此这里按包精确登记，与其余模块一致。
 *
 * 之前多写了一个 `annotationClass = Mapper.class`：它会把这些包里任何带 @Mapper 的接口
 * 都注册成 MyBatis 映射器 —— 包括将来可能引入的 MapStruct @Mapper（那是个同名不同义的注解），
 * 而且一旦某个 Mapper 忘了写 @Mapper，它会静默漏注册、直到运行期才报 "Invalid bound statement"。
 * 按包扫描没有这两个问题，行为与其它模块也完全一致。
 */
/**
 * 英语持久化配置，属于英语学习模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@MapperScan({
    "com.starrainnotes.english.overview.mapper",
    "com.starrainnotes.english.vocabulary.mapper",
    "com.starrainnotes.english.grammar.mapper",
    "com.starrainnotes.english.reading.mapper",
    "com.starrainnotes.english.writing.article.mapper",
    "com.starrainnotes.english.taxonomy.mapper",
    "com.starrainnotes.english.knowledge.mapper"
})
public class EnglishPersistenceConfig {
}
