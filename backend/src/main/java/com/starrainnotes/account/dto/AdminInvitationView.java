package com.starrainnotes.account.dto;

import com.starrainnotes.account.entity.AdminInvitation;

import java.time.LocalDateTime;

public record AdminInvitationView(
        Long id,
        String email,
        String status,
        Long invitedBy,
        Long acceptedAccountId,
        LocalDateTime expiresAt,
        LocalDateTime sentAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AdminInvitationView from(AdminInvitation invitation) {
        return new AdminInvitationView(invitation.getId(), invitation.getEmail(), invitation.getStatus(),
                invitation.getInvitedBy(), invitation.getAcceptedAccountId(), invitation.getExpiresAt(),
                invitation.getSentAt(), invitation.getAcceptedAt(), invitation.getRevokedAt(),
                invitation.getCreatedAt(), invitation.getUpdatedAt());
    }
}
