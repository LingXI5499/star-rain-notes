package com.starrainnotes.account.service;

import com.starrainnotes.account.vo.EmailVerificationCodeVO;

// 邮箱验证码的发送、校验与消费入口。
public interface EmailVerificationService {

    EmailVerificationCodeVO send(String rawEmail, String ip);

    EmailVerificationCodeVO sendForCurrentAccount(String ip);

    String verify(String email, String code);

    void consume(String email, String codeHash);
}
