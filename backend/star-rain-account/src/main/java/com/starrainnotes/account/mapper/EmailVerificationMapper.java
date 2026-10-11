package com.starrainnotes.account.mapper;

import com.starrainnotes.account.entity.EmailVerificationEntity;
import org.apache.ibatis.annotations.Param;

public interface EmailVerificationMapper {
    void ensureEmail(@Param("email") String email);

    EmailVerificationEntity findForUpdate(@Param("email") String email);

    void saveCode(EmailVerificationEntity verification);

    void incrementFailures(@Param("email") String email);

    int consume(@Param("email") String email, @Param("codeHash") String codeHash);
}
