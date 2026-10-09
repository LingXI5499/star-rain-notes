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
public interface ProfileMapper {

    ProfileEntity selectOwner();

    /* 显示名称、标题、个人介绍、所在地；不动头像与简历引用 */
    int updateBasic(ProfileEntity profile);

    /* 头像与简历引用：两条列都按 ALWAYS 语义写回，其中一条保持原值 */
    int updateMediaAssets(ProfileEntity profile);
}
