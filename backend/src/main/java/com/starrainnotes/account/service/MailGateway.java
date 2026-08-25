package com.starrainnotes.account.service;

/** Abstraction over outbound email. Tests substitute a capturing implementation. */
public interface MailGateway {
    void sendVerificationCode(String to, String purpose, String code);
    void sendInvitationLink(String to, String link);
}