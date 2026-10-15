package com.starrainnotes.account.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Internal reservation receipt; never serialized by a controller. */
@Getter
@AllArgsConstructor
public class LoginReservation {
    /** HMAC source key and its window identify only this reservation's counters. */
    private final String sourceKey;
    private final LocalDateTime sourceWindow;
    /** HMAC account key and its window prevent a late success clearing a newer window. */
    private final String identityKey;
    private final LocalDateTime identityWindow;
}
