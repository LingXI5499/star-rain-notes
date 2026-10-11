/*
 * ProfileAdminController：作者资料模块的HTTP 接入文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、ExperienceDTO、FeaturedContentDTO、ProfileMediaDTO、ProfileOrderDTO、ProfilePatchDTO、SkillDTO。
 * 阅读入口：get（读取资料后台）；basic（basic处理入口）；addExperience（添加Experience）；editExperience（editExperience处理入口）。
 */

package com.starrainnotes.profile.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.profile.dto.ExperienceDTO;
import com.starrainnotes.profile.dto.FeaturedContentDTO;
import com.starrainnotes.profile.dto.ProfileMediaDTO;
import com.starrainnotes.profile.dto.ProfileOrderDTO;
import com.starrainnotes.profile.dto.ProfilePatchDTO;
import com.starrainnotes.profile.dto.SkillDTO;
import com.starrainnotes.profile.dto.SocialLinkDTO;
import com.starrainnotes.profile.service.ProfileService;
import com.starrainnotes.profile.api.vo.ProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资料后台控制器，属于作者资料模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/profile")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ProfileAdminController {
    /**
     * 业务服务。
     * 协作类型：<code>ProfileService</code>。
     */
    private final ProfileService service;

    /**
     * 读取资料后台。
     * HTTP 映射：<code>@GetMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:read-admin\')")</code>。
     * 主要协作调用：<code>service.adminProfile</code>。
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @GetMapping
    @PreAuthorize("hasAuthority('profile:read-admin')")
    public ApiResponse<ProfileVO> get() { return ApiResponse.ok(service.adminProfile()); }

    /**
     * basic处理入口。
     * HTTP 映射：<code>@PatchMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.updateBasic</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ProfilePatchDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PatchMapping
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> basic(@RequestBody ProfilePatchDTO request) {
        return ApiResponse.ok(service.updateBasic(request));
    }

    /**
     * 添加Experience。
     * HTTP 映射：<code>@PostMapping("/experiences")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.saveExperience</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ExperienceDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PostMapping("/experiences")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> addExperience(@RequestBody ExperienceDTO request) {
        return ApiResponse.ok(service.saveExperience(null, request));
    }

    /**
     * editExperience处理入口。
     * HTTP 映射：<code>@PatchMapping("/experiences/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.saveExperience</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ExperienceDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PatchMapping("/experiences/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> editExperience(@PathVariable Long id, @RequestBody ExperienceDTO request) {
        return ApiResponse.ok(service.saveExperience(id, request));
    }

    /**
     * 删除Experience。
     * HTTP 映射：<code>@DeleteMapping("/experiences/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.removeExperience</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @DeleteMapping("/experiences/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> deleteExperience(@PathVariable Long id) {
        return ApiResponse.ok(service.removeExperience(id));
    }

    /**
     * 顺序Experiences处理入口。
     * HTTP 映射：<code>@PutMapping("/experiences/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.orderExperiences</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ProfileOrderDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PutMapping("/experiences/order")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> orderExperiences(@RequestBody ProfileOrderDTO request) {
        return ApiResponse.ok(service.orderExperiences(request.getIds()));
    }

    /**
     * 添加Skill。
     * HTTP 映射：<code>@PostMapping("/skills")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.saveSkill</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>SkillDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PostMapping("/skills")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> addSkill(@RequestBody SkillDTO request) {
        return ApiResponse.ok(service.saveSkill(null, request));
    }

    /**
     * editSkill处理入口。
     * HTTP 映射：<code>@PatchMapping("/skills/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.saveSkill</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>SkillDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PatchMapping("/skills/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> editSkill(@PathVariable Long id, @RequestBody SkillDTO request) {
        return ApiResponse.ok(service.saveSkill(id, request));
    }

    /**
     * 删除Skill。
     * HTTP 映射：<code>@DeleteMapping("/skills/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.removeSkill</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @DeleteMapping("/skills/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> deleteSkill(@PathVariable Long id) {
        return ApiResponse.ok(service.removeSkill(id));
    }

    /**
     * 顺序Skills处理入口。
     * HTTP 映射：<code>@PutMapping("/skills/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.orderSkills</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ProfileOrderDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PutMapping("/skills/order")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> orderSkills(@RequestBody ProfileOrderDTO request) {
        return ApiResponse.ok(service.orderSkills(request.getIds()));
    }

    /**
     * 添加Social。
     * HTTP 映射：<code>@PostMapping("/social-links")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.saveSocial</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>SocialLinkDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PostMapping("/social-links")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> addSocial(@RequestBody SocialLinkDTO request) {
        return ApiResponse.ok(service.saveSocial(null, request));
    }

    /**
     * editSocial处理入口。
     * HTTP 映射：<code>@PatchMapping("/social-links/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.saveSocial</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>SocialLinkDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PatchMapping("/social-links/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> editSocial(@PathVariable Long id, @RequestBody SocialLinkDTO request) {
        return ApiResponse.ok(service.saveSocial(id, request));
    }

    /**
     * 删除Social。
     * HTTP 映射：<code>@DeleteMapping("/social-links/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.removeSocial</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @DeleteMapping("/social-links/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> deleteSocial(@PathVariable Long id) {
        return ApiResponse.ok(service.removeSocial(id));
    }

    /**
     * 顺序Social处理入口。
     * HTTP 映射：<code>@PutMapping("/social-links/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.orderSocial</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ProfileOrderDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PutMapping("/social-links/order")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> orderSocial(@RequestBody ProfileOrderDTO request) {
        return ApiResponse.ok(service.orderSocial(request.getIds()));
    }

    /**
     * 设置媒体。
     * HTTP 映射：<code>@PutMapping("/{kind:avatar|resume}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.setMedia</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ProfileMediaDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PutMapping("/{kind:avatar|resume}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> setMedia(@PathVariable String kind, @RequestBody ProfileMediaDTO request) {
        return ApiResponse.ok(service.setMedia(kind, request.getMediaAssetId()));
    }

    /**
     * 清理媒体。
     * HTTP 映射：<code>@DeleteMapping("/{kind:avatar|resume}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.setMedia</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @DeleteMapping("/{kind:avatar|resume}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> clearMedia(@PathVariable String kind) {
        return ApiResponse.ok(service.setMedia(kind, null));
    }

    /**
     * 添加精选。
     * HTTP 映射：<code>@PostMapping("/featured")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.addFeatured</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>FeaturedContentDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PostMapping("/featured")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> addFeatured(@RequestBody FeaturedContentDTO request) {
        return ApiResponse.ok(service.addFeatured(request));
    }

    /**
     * 删除精选。
     * HTTP 映射：<code>@DeleteMapping("/featured/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.removeFeatured</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @DeleteMapping("/featured/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> deleteFeatured(@PathVariable Long id) {
        return ApiResponse.ok(service.removeFeatured(id));
    }

    /**
     * 顺序精选处理入口。
     * HTTP 映射：<code>@PutMapping("/featured/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'profile:edit\')")</code>。
     * 主要协作调用：<code>service.orderFeatured</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ProfileOrderDTO</code>
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @PutMapping("/featured/order")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> orderFeatured(@RequestBody ProfileOrderDTO request) {
        return ApiResponse.ok(service.orderFeatured(request.getIds()));
    }
}
