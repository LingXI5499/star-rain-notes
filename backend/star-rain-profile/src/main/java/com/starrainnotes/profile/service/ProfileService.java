/*
 * ProfileService：作者资料模块的业务契约文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：ExperienceDTO、FeaturedContentDTO、ProfilePatchDTO、SkillDTO、SocialLinkDTO、ProfileVO。
 * 阅读入口：publicProfile（公开资料处理入口）；adminProfile（后台资料处理入口）；updateBasic（更新Basic）；saveExperience（保存Experience）。
 */

package com.starrainnotes.profile.service;

import com.starrainnotes.profile.dto.ExperienceDTO;
import com.starrainnotes.profile.dto.FeaturedContentDTO;
import com.starrainnotes.profile.dto.ProfilePatchDTO;
import com.starrainnotes.profile.dto.SkillDTO;
import com.starrainnotes.profile.dto.SocialLinkDTO;
import com.starrainnotes.profile.api.vo.ProfileVO;
import java.util.List;

/**
 * 资料业务服务，属于作者资料模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface ProfileService {
    /**
     * 公开资料处理入口。
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO publicProfile();
    /**
     * 后台资料处理入口。
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO adminProfile();
    /**
     * 更新Basic。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ProfilePatchDTO</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO updateBasic(ProfilePatchDTO request);
    /**
     * 保存Experience。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ExperienceDTO</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO saveExperience(Long id, ExperienceDTO request);
    /**
     * 移除Experience。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO removeExperience(Long id);
    /**
     * 顺序Experiences处理入口。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO orderExperiences(List<Long> ids);
    /**
     * 保存Skill。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>SkillDTO</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO saveSkill(Long id, SkillDTO request);
    /**
     * 移除Skill。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO removeSkill(Long id);
    /**
     * 顺序Skills处理入口。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO orderSkills(List<Long> ids);
    /**
     * 保存Social。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>SocialLinkDTO</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO saveSocial(Long id, SocialLinkDTO request);
    /**
     * 移除Social。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO removeSocial(Long id);
    /**
     * 顺序Social处理入口。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO orderSocial(List<Long> ids);
    /**
     * 设置媒体。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO setMedia(String kind, Long mediaAssetId);
    /**
     * 添加精选。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>FeaturedContentDTO</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO addFeatured(FeaturedContentDTO request);
    /**
     * 移除精选。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO removeFeatured(Long id);
    /**
     * 顺序精选处理入口。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO orderFeatured(List<Long> ids);
}
