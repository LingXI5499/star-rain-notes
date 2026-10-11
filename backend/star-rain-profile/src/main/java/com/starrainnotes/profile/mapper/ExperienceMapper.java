/*
 * ExperienceMapper：作者资料模块的数据访问文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：ExperienceEntity。
 * 阅读入口：insert（写入Experience）；selectById（按标识读取查询）；listByProfileId（列出依据资料标识）；listEnabledByProfileId（列出已启用依据资料标识）。
 */

package com.starrainnotes.profile.mapper;

import com.starrainnotes.profile.entity.ExperienceEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作者教育/经历。SQL 全部在 mapper/profile/ExperienceMapper.xml。
 *
 * 「保存经历」原先走 updateById，会把读出来的整行连同不可变的 profile_id 一起写回；
 * 迁移后 updateContent 只列这条路径真正可能改动的列，profile_id 不参与 SET。
 */
/**
 * Experience数据映射，属于作者资料模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface ExperienceMapper {

    /**
     * 写入Experience。
     *
     * 持久化操作：新增，涉及 <code>sr_profile_experience</code>。
     *
     * @param experience experience；类型为 <code>ExperienceEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(ExperienceEntity experience);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_experience</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>ExperienceEntity</code> 的处理结果
     */
    ExperienceEntity selectById(@Param("id") Long id);

    /* 后台排序用：该 profile 下的全部条目，不按 status 过滤 */
    /**
     * 列出依据资料标识。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_experience</code>。
     *
     * @param profileId 资料标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("profileId")</code>
     * @return 本次处理得到的条目集合
     */
    List<ExperienceEntity> listByProfileId(@Param("profileId") Long profileId);

    /* 公开/后台展示用：只取 ENABLED */
    /**
     * 列出已启用依据资料标识。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_experience</code>。
     *
     * @param profileId 资料标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("profileId")</code>
     * @return 本次处理得到的条目集合
     */
    List<ExperienceEntity> listEnabledByProfileId(@Param("profileId") Long profileId);

    /* 新增时用来取「当前条目数」当初始排序值 */
    /**
     * 统计依据资料标识。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_experience</code>。
     *
     * @param profileId 资料标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("profileId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByProfileId(@Param("profileId") Long profileId);

    /**
     * 更新内容。
     *
     * 持久化操作：更新，涉及 <code>sr_profile_experience</code>。
     *
     * @param experience experience；类型为 <code>ExperienceEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateContent(ExperienceEntity experience);

    /**
     * 更新排序顺序。
     *
     * 持久化操作：更新，涉及 <code>sr_profile_experience</code>。
     *
     * @param experience experience；类型为 <code>ExperienceEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateSortOrder(ExperienceEntity experience);

    /**
     * 删除依据标识。
     *
     * 持久化操作：删除，涉及 <code>sr_profile_experience</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
