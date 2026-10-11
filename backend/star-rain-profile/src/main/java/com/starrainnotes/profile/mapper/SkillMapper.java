/*
 * SkillMapper：作者资料模块的数据访问文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：SkillEntity。
 * 阅读入口：insert（写入Skill）；selectById（按标识读取查询）；listByProfileId（列出依据资料标识）；listEnabledByProfileId（列出已启用依据资料标识）。
 */

package com.starrainnotes.profile.mapper;

import com.starrainnotes.profile.entity.SkillEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作者技能 / 方向 / 兴趣。SQL 全部在 mapper/profile/SkillMapper.xml。
 *
 * description / proficiency 原先带 @TableField(updateStrategy = FieldStrategy.ALWAYS)，
 * 「清空说明」必须能把列写成 NULL，这套语义由 SkillMapper.xml 的 updateContent 承担。
 */
/**
 * Skill数据映射，属于作者资料模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface SkillMapper {

    /**
     * 写入Skill。
     *
     * 持久化操作：新增，涉及 <code>sr_profile_skill</code>。
     *
     * @param skill skill；类型为 <code>SkillEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(SkillEntity skill);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_skill</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>SkillEntity</code> 的处理结果
     */
    SkillEntity selectById(@Param("id") Long id);

    /**
     * 列出依据资料标识。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_skill</code>。
     *
     * @param profileId 资料标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("profileId")</code>
     * @return 本次处理得到的条目集合
     */
    List<SkillEntity> listByProfileId(@Param("profileId") Long profileId);

    /**
     * 列出已启用依据资料标识。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_skill</code>。
     *
     * @param profileId 资料标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("profileId")</code>
     * @return 本次处理得到的条目集合
     */
    List<SkillEntity> listEnabledByProfileId(@Param("profileId") Long profileId);

    /**
     * 统计依据资料标识。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_skill</code>。
     *
     * @param profileId 资料标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("profileId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByProfileId(@Param("profileId") Long profileId);

    /**
     * 更新内容。
     *
     * 持久化操作：更新，涉及 <code>sr_profile_skill</code>。
     *
     * @param skill skill；类型为 <code>SkillEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateContent(SkillEntity skill);

    /**
     * 更新排序顺序。
     *
     * 持久化操作：更新，涉及 <code>sr_profile_skill</code>。
     *
     * @param skill skill；类型为 <code>SkillEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateSortOrder(SkillEntity skill);

    /**
     * 删除依据标识。
     *
     * 持久化操作：删除，涉及 <code>sr_profile_skill</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
