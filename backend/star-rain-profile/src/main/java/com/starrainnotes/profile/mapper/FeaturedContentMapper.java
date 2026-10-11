/*
 * FeaturedContentMapper：作者资料模块的数据访问文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：FeaturedContentEntity。
 * 阅读入口：insert（写入精选内容）；selectById（按标识读取查询）；listByProfileId（列出依据资料标识）；listEnabledByProfileId（列出已启用依据资料标识）。
 */

package com.starrainnotes.profile.mapper;

import com.starrainnotes.profile.entity.FeaturedContentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作者精选内容引用。SQL 全部在 mapper/profile/FeaturedContentMapper.xml。
 *
 * 这张表的业务只有「新增一条引用」「删除」「调整顺序」：content_type / content_id 是引用的身份，
 * title_override 只在新增时写入，因此没有 updateContent，只有 updateSortOrder。
 */
/**
 * 精选内容数据映射，属于作者资料模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface FeaturedContentMapper {

    /**
     * 写入精选内容。
     *
     * 持久化操作：新增，涉及 <code>sr_profile_featured_content</code>。
     *
     * @param featuredContent 精选内容；类型为 <code>FeaturedContentEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(FeaturedContentEntity featuredContent);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_featured_content</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>FeaturedContentEntity</code> 的处理结果
     */
    FeaturedContentEntity selectById(@Param("id") Long id);

    /**
     * 列出依据资料标识。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_featured_content</code>。
     *
     * @param profileId 资料标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("profileId")</code>
     * @return 本次处理得到的条目集合
     */
    List<FeaturedContentEntity> listByProfileId(@Param("profileId") Long profileId);

    /**
     * 列出已启用依据资料标识。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_featured_content</code>。
     *
     * @param profileId 资料标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("profileId")</code>
     * @return 本次处理得到的条目集合
     */
    List<FeaturedContentEntity> listEnabledByProfileId(@Param("profileId") Long profileId);

    /**
     * 统计依据资料标识。
     *
     * 持久化操作：读取，涉及 <code>sr_profile_featured_content</code>。
     *
     * @param profileId 资料标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("profileId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByProfileId(@Param("profileId") Long profileId);

    /**
     * 更新排序顺序。
     *
     * 持久化操作：更新，涉及 <code>sr_profile_featured_content</code>。
     *
     * @param featuredContent 精选内容；类型为 <code>FeaturedContentEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateSortOrder(FeaturedContentEntity featuredContent);

    /**
     * 删除依据标识。
     *
     * 持久化操作：删除，涉及 <code>sr_profile_featured_content</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
