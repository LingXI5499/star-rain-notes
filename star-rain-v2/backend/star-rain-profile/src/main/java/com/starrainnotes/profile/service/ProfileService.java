package com.starrainnotes.profile.service;

import com.starrainnotes.profile.dto.ExperienceDTO;
import com.starrainnotes.profile.dto.FeaturedContentDTO;
import com.starrainnotes.profile.dto.ProfilePatchDTO;
import com.starrainnotes.profile.dto.SkillDTO;
import com.starrainnotes.profile.dto.SocialLinkDTO;
import com.starrainnotes.profile.vo.ProfileVO;
import java.util.List;

public interface ProfileService {
    ProfileVO publicProfile();
    ProfileVO adminProfile();
    ProfileVO updateBasic(ProfilePatchDTO request);
    ProfileVO saveExperience(Long id, ExperienceDTO request);
    ProfileVO removeExperience(Long id);
    ProfileVO orderExperiences(List<Long> ids);
    ProfileVO saveSkill(Long id, SkillDTO request);
    ProfileVO removeSkill(Long id);
    ProfileVO orderSkills(List<Long> ids);
    ProfileVO saveSocial(Long id, SocialLinkDTO request);
    ProfileVO removeSocial(Long id);
    ProfileVO orderSocial(List<Long> ids);
    ProfileVO setMedia(String kind, Long mediaAssetId);
    ProfileVO addFeatured(FeaturedContentDTO request);
    ProfileVO removeFeatured(Long id);
    ProfileVO orderFeatured(List<Long> ids);
}
