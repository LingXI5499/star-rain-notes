package com.starrainnotes.tutorial.dto;

/**
 * Previous/next chapter in depth-first preorder (skips hidden chapters).
 */
public record PrevNextView(
        Long chapterId,
        String chapterSlug,
        String chapterTitle) {
}
