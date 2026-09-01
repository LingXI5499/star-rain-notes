package com.starrainnotes.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.account.entity.EmailVerificationChallenge;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailVerificationChallengeMapper extends BaseMapper<EmailVerificationChallenge> {
}