package com.starrainnotes.account.service;

public interface AccountMailService {
    void requireDelivery();

    String invitationUrl(String token);

    void sendReset(String email, String token);

    void sendEmailVerificationCode(String email, String code);

    String sendInvitation(String email, String token);
}
