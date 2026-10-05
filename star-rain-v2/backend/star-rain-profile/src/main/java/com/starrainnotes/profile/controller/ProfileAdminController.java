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
import com.starrainnotes.profile.api.dto.ProfileVO;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/profile")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ProfileAdminController {
    private final ProfileService service;

    @GetMapping
    @PreAuthorize("hasAuthority('profile:read-admin')")
    public ApiResponse<ProfileVO> get() { return ApiResponse.ok(service.adminProfile()); }

    @PatchMapping
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> basic(@RequestBody ProfilePatchDTO request) {
        return ApiResponse.ok(service.updateBasic(request));
    }

    @PostMapping("/experiences")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> addExperience(@RequestBody ExperienceDTO request) {
        return ApiResponse.ok(service.saveExperience(null, request));
    }

    @PatchMapping("/experiences/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> editExperience(@PathVariable Long id, @RequestBody ExperienceDTO request) {
        return ApiResponse.ok(service.saveExperience(id, request));
    }

    @DeleteMapping("/experiences/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> deleteExperience(@PathVariable Long id) {
        return ApiResponse.ok(service.removeExperience(id));
    }

    @PutMapping("/experiences/order")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> orderExperiences(@RequestBody ProfileOrderDTO request) {
        return ApiResponse.ok(service.orderExperiences(request.getIds()));
    }

    @PostMapping("/skills")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> addSkill(@RequestBody SkillDTO request) {
        return ApiResponse.ok(service.saveSkill(null, request));
    }

    @PatchMapping("/skills/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> editSkill(@PathVariable Long id, @RequestBody SkillDTO request) {
        return ApiResponse.ok(service.saveSkill(id, request));
    }

    @DeleteMapping("/skills/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> deleteSkill(@PathVariable Long id) {
        return ApiResponse.ok(service.removeSkill(id));
    }

    @PutMapping("/skills/order")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> orderSkills(@RequestBody ProfileOrderDTO request) {
        return ApiResponse.ok(service.orderSkills(request.getIds()));
    }

    @PostMapping("/social-links")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> addSocial(@RequestBody SocialLinkDTO request) {
        return ApiResponse.ok(service.saveSocial(null, request));
    }

    @PatchMapping("/social-links/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> editSocial(@PathVariable Long id, @RequestBody SocialLinkDTO request) {
        return ApiResponse.ok(service.saveSocial(id, request));
    }

    @DeleteMapping("/social-links/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> deleteSocial(@PathVariable Long id) {
        return ApiResponse.ok(service.removeSocial(id));
    }

    @PutMapping("/social-links/order")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> orderSocial(@RequestBody ProfileOrderDTO request) {
        return ApiResponse.ok(service.orderSocial(request.getIds()));
    }

    @PutMapping("/{kind:avatar|resume}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> setMedia(@PathVariable String kind, @RequestBody ProfileMediaDTO request) {
        return ApiResponse.ok(service.setMedia(kind, request.getMediaAssetId()));
    }

    @DeleteMapping("/{kind:avatar|resume}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> clearMedia(@PathVariable String kind) {
        return ApiResponse.ok(service.setMedia(kind, null));
    }

    @PostMapping("/featured")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> addFeatured(@RequestBody FeaturedContentDTO request) {
        return ApiResponse.ok(service.addFeatured(request));
    }

    @DeleteMapping("/featured/{id}")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> deleteFeatured(@PathVariable Long id) {
        return ApiResponse.ok(service.removeFeatured(id));
    }

    @PutMapping("/featured/order")
    @PreAuthorize("hasAuthority('profile:edit')")
    public ApiResponse<ProfileVO> orderFeatured(@RequestBody ProfileOrderDTO request) {
        return ApiResponse.ok(service.orderFeatured(request.getIds()));
    }
}
