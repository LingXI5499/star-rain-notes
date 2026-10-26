package com.starrainnotes.account.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.starrainnotes.account.delivery.MailOutbox;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class AccountMailServiceLinksTest {
    @Test
    void mailLinksUseCanonicalRoutesAndKeepTokens() {
        MailOutbox outbox = mock(MailOutbox.class);
        AccountMailServiceImpl mail = new AccountMailServiceImpl(mock(JavaMailSender.class), outbox,
                true, "sender@example.test", "https://notes.example.test/", true);
        assertEquals("https://notes.example.test/reset-password?token=abc-123_xyz", mail.resetUrl("abc-123_xyz"));
        assertEquals("https://notes.example.test/invitation/accept?token=abc-123_xyz", mail.invitationUrl("abc-123_xyz"));
        mail.requestReset("reader@example.test");
        verify(outbox).enqueueResetRequest("sender@example.test", "reader@example.test", "https://notes.example.test");
    }
}
