package com.starrainnotes.english.shared.events;

/** Owning module lookup of slug and publish status. Missing rows return null. */
public interface EnglishContentStateSource {
    EnglishContentKind kind();

    EnglishContentState find(long id);
}
