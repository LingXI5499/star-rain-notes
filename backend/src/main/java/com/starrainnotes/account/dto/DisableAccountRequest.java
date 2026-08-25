package com.starrainnotes.account.dto;

import jakarta.validation.constraints.Size;

public record DisableAccountRequest(@Size(max = 500) String reason) {
}
