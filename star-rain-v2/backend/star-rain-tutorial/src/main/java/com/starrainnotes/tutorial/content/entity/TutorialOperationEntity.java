package com.starrainnotes.tutorial.content.entity;

import lombok.Data;

/** Frozen operation proposal; business data is changed only after approval. */
@Data
public class TutorialOperationEntity {
    private Long id;
    private String operation;
    private String argumentsJson;
    private String baselineJson;
    private String scopeKey;
    private String pendingScope;
    private String status;
    private String displayName;
    private Long applicantAccountId;
}
