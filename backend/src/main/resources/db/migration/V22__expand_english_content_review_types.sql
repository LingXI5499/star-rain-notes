ALTER TABLE content_review_request
    DROP CHECK chk_content_review_type;

ALTER TABLE content_review_request
    ADD CONSTRAINT chk_content_review_type
        CHECK (content_type IN (
            'BLOG_POST',
            'TUTORIAL_CHAPTER',
            'ENGLISH_GRAMMAR_LESSON',
            'ENGLISH_READING_ARTICLE',
            'ENGLISH_LISTENING_ITEM',
            'ENGLISH_PRONUNCIATION_RULE',
            'ENGLISH_WRITING_RESOURCE',
            'ENGLISH_WRITING_PROMPT'
        ));
