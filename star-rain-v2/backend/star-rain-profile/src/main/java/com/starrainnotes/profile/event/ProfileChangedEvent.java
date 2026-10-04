package com.starrainnotes.profile.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileChangedEvent {
    private Long profileId;
}
