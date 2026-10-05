package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 教程工作区的全部 SQL 都在 mapper/tutorial/TutorialMapper.xml。
 *
 * 本接口不再继承 MyBatis-Plus 的 BaseMapper：selectById / insert / updateById 这些语句
 * 原先由框架在运行期拼接，源码里看不到实际 SQL。逐个访问形状显式声明后，
 * 每个方法都能在 XML 里找到唯一对应的一条语句（star-rain-boot 的 MapperXmlIntegrationTest 强制校验）。
 */
@Mapper
public interface TutorialMapper {

    int insert(TutorialEntity tutorial);

    TutorialEntity selectById(@Param("id") Long id);

    /* 工作区写路径先锁行：发布、撤回、结构变更都要求同一行串行。 */
    TutorialEntity byIdForUpdate(@Param("id") Long id);

    /* 公开读取只认 PUBLISHED，且必须已经冻结过版本。 */
    TutorialEntity selectPublishedBySlug(@Param("slug") String slug);

    /* categoryId 为 null 表示「全部知识体系」，用于后台列表。 */
    List<TutorialEntity> listOrdered(@Param("categoryId") Long categoryId);

    /* 公开目录：已发布且已有公开版本。 */
    List<TutorialEntity> listPublishedWithRevision();

    /* 学习侧按「已发布」扫描公开快照，不额外要求 published_revision_id 非空。 */
    List<TutorialEntity> listPublished();

    long countBySlug(@Param("slug") String slug);

    long countByCategoryId(@Param("categoryId") Long categoryId);

    /* 整行保存工作区改动；withdrawn_at 必须写进 SET，null 也要覆盖（旧 @TableField(ALWAYS) 语义）。 */
    int update(TutorialEntity tutorial);

    int deleteById(@Param("id") Long id);
}
