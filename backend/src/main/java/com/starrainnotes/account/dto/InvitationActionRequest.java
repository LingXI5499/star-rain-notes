package com.starrainnotes.account.dto;

import jakarta.validation.constraints.NotNull;

public record InvitationActionRequest(@NotNull Long invitationId) {
}