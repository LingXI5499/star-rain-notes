package com.starrainnotes.vocabulary.entity;

/**
 * One user-added example sentence stored inside the word's {@code examples}
 * JSON array. Kept as a plain mutable class so Jackson and MyBatis can
 * round-trip it through the JSON column.
 */
public class VocabularyExample {

    private String sentence;
    private String translation;

    public VocabularyExample() {
    }

    public VocabularyExample(String sentence, String translation) {
        this.sentence = sentence;
        this.translation = translation;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }
}
