package com.starrainnotes.portfolio.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkPublicationChangedEvent {
    private Long workId;
    private String slug;
    private boolean published;
}
