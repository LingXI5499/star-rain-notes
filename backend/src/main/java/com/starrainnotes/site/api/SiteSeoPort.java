package com.starrainnotes.site.api;

public interface SiteSeoPort {
    record Identity(String name, String tagline, String defaultSeoDescription, String githubUrl) {
    }

    Identity identity();
}
