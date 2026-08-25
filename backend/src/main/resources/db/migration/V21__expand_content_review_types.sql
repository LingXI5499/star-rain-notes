ALTER TABLE content_review_request
    DROP CHECK chk_content_review_type;

ALTER TABLE content_review_request
    ADD CONSTRAINT chk_content_review_type
        CHECK (content_type IN ('BLOG_POST','TUTORIAL_CHAPTER'));
