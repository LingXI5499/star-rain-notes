package com.starrainnotes.account.controller;

import com.starrainnotes.account.dto.InvitationStatusView;
import com.starrainnotes.account.dto.RegisterRequest;
import com.starrainnotes.account.entity.AdminInvitation;
import com.starrainnotes.account.service.AccountInvitationService;
import com.starrainnotes.account.service.AccountService;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/auth/invitations")
public class AccountInvitationController {

    private final AccountInvitationService invitations;
    private final SiteSettingsTimezone timezone;

    public AccountInvitationController(AccountInvitationService invitations, SiteSettingsTimezone timezone) {
        this.invitations = invitations;
        this.timezone = timezone;
    }

    @GetMapping("/{token}")
    public InvitationStatusView status(@PathVariable String token) {
        AdminInvitation inv = invitations.byToken(token);
        return new InvitationStatusView(AccountService.mask(inv.getEmail()), inv.getStatus(),
                timezone.atSite(inv.getExpiresAt()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    @PostMapping("/{token}/verification-codes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendCode(@PathVariable String token, HttpServletRequest request) {
        invitations.requestCode(token, AccountActivationController.clientIp(request));
    }

    @PostMapping("/{token}/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@PathVariable String token, @Valid @RequestBody RegisterRequest body) {
        invitations.register(token, body.email(), body.verificationCode(), body.password());
    }
}
