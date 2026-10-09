package com.starrainnotes.profile.api.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileChangedEvent {
    private Long profileId;
}
