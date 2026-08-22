package com.starrainnotes.auth.dto;

/**
 * GET /api/v1/setup/status response.
 */
public record SetupStatusView(boolean setupRequired) {
}
