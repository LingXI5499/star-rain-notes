package com.starrainnotes.portfolio.dto;

/** Previous/next portfolio entry in the public case-study order. */
public record PrevNextProjectView(Long projectId, String slug, String title) {
}
