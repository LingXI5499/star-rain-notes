/*
 * ProfileMapper：作者资料模块的数据访问文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：ProfileEntity。
 * 阅读入口：selectOwner（查询Owner处理入口）；updateBasic（更新Basic）；updateMediaAssets（更新媒体Assets）。
 */

package com.starrainnotes.profile.mapper;

import com.starrainnotes.profile.entity.ProfileEntity;

/*
 * 作者公开 Profile（全库单行）。SQL 全部在 mapper/profile/ProfileMapper.xml。
 *
 * 不再继承 MyBatis-Plus 的 BaseMapper：这行资料只有「读 OWNER 行」「改基础信息」
 * 「换头像/简历媒体」三种访问形状，显式声明后每个方法都能在 XML 里找到唯一的语句。
 *
 * 原先 updateById 依赖 @TableField(updateStrategy = FieldStrategy.ALWAYS) 才能把
 * headline / bioMarkdown / locationText / avatarMediaAssetId / resumeMediaAssetId
 * 写成 NULL（清空）。这套语义现在由 XML 里无条件的 SET 承担，见 ProfileMapper.xml。
 */
/**
 * 资料数据映射，属于作者资料模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface ProfileMapper {

    /**
     * 查询Owner处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_profile</code>。
     * @return 类型为 <code>ProfileEntity</code> 的处理结果
     */
    ProfileEntity selectOwner();

    /* 显示名称、标题、个人介绍、所在地；不动头像与简历引用 */
    /**
     * 更新Basic。
     *
     * 持久化操作：更新，涉及 <code>sr_profile</code>。
     *
     * @param profile 资料；类型为 <code>ProfileEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateBasic(ProfileEntity profile);

    /* 头像与简历引用：两条列都按 ALWAYS 语义写回，其中一条保持原值 */
    /**
     * 更新媒体Assets。
     *
     * 持久化操作：更新，涉及 <code>sr_profile</code>。
     *
     * @param profile 资料；类型为 <code>ProfileEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateMediaAssets(ProfileEntity profile);
}
