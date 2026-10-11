/*
 * EnglishOverviewMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：EnglishOverviewEntity。
 * 阅读入口：selectSingleton（查询Singleton处理入口）；updateContent（更新内容）。
 */

package com.starrainnotes.english.overview.mapper;

import com.starrainnotes.english.overview.entity.EnglishOverviewEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 英语概览（全表单行，CHECK (id = 1)）。SQL 全部在 mapper/english/EnglishOverviewMapper.xml。
 *
 * 不再继承 MyBatis-Plus 的 BaseMapper：这张表只有「读唯一那行」和「改文案」两种访问形状，
 * 原先的 selectById(1) 由框架运行期拼装，源码里看不到实际 SQL。
 */
/**
 * 英语Overview数据映射，属于英语学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface EnglishOverviewMapper {

    /**
     * 查询Singleton处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_overview</code>。
     * @return 类型为 <code>EnglishOverviewEntity</code> 的处理结果
     */
    EnglishOverviewEntity selectSingleton();

    /**
     * 更新内容。
     *
     * 持久化操作：更新，涉及 <code>sr_english_overview</code>。
     *
     * @param title 标题；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("title")</code>
     * @param subtitle 副标题；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("subtitle")</code>
     * @param introduction 介绍；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("introduction")</code>
     * @param roadmapMarkdown 路线Markdown；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("roadmapMarkdown")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateContent(@Param("title") String title, @Param("subtitle") String subtitle,
                      @Param("introduction") String introduction,
                      @Param("roadmapMarkdown") String roadmapMarkdown);
}
