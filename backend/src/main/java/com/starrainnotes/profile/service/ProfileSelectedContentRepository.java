package com.starrainnotes.profile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.profile.entity.ProfileSelectedContent;
import com.starrainnotes.profile.mapper.ProfileSelectedContentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProfileSelectedContentRepository {
    private static final int PROFILE_ID = 1;
    private final ProfileSelectedContentMapper mapper;

    public ProfileSelectedContentRepository(ProfileSelectedContentMapper mapper) {
        this.mapper = mapper;
    }

    public List<ProfileSelectedContent> list() {
        return mapper.selectList(new LambdaQueryWrapper<ProfileSelectedContent>()
                .eq(ProfileSelectedContent::getProfileId, PROFILE_ID)
                .orderByAsc(ProfileSelectedContent::getSortOrder)
                .orderByAsc(ProfileSelectedContent::getId));
    }

    public void replace(List<ProfileSelectedContent> rows) {
        mapper.delete(new LambdaQueryWrapper<ProfileSelectedContent>()
                .eq(ProfileSelectedContent::getProfileId, PROFILE_ID));
        for (ProfileSelectedContent row : rows) {
            mapper.insert(row);
        }
    }
}
