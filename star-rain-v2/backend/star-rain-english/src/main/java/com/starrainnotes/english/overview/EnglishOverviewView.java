package com.starrainnotes.english.overview;

public record EnglishOverviewView(String title, String subtitle, String introduction,
                                  String currentStage, String roadmapMarkdown) {
    public static EnglishOverviewView from(EnglishOverviewEntity entity) {
        return new EnglishOverviewView(entity.getTitle(), entity.getSubtitle(),
                entity.getIntroduction(), entity.getCurrentStage(), entity.getRoadmapMarkdown());
    }
}
