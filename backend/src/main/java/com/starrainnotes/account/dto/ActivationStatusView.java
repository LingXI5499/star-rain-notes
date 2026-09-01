package com.starrainnotes.account.dto;

public record ActivationStatusView(boolean configured, boolean activated, String emailMasked) {
}