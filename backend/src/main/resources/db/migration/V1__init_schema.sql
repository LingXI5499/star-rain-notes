-- =====================================================================
-- Star Rain Notes V1 — V1__init_schema.sql
-- Frozen physical model baseline (03-database-design.md §8).
-- MySQL 8.0.26 / InnoDB / utf8mb4 / utf8mb4_0900_ai_ci / DATETIME(3).
-- Exactly 13 core business tables. No ENUM. No logical delete.
-- Flyway owns schema changes.
-- =====================================================================

-- ---------------------------------------------------------------
-- 1. admin_user — single administrator (seeded by setup flow, never defaulted)
-- ---------------------------------------------------------------
CREATE TABLE admin_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    last_login_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_user_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 2. media_asset — storage_path is a RELATIVE storage key, never an
--    absolute machine root path (AGENTS.md / 03 §7)
-- ---------------------------------------------------------------
CREATE TABLE media_asset (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_type VARCHAR(20) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    extension VARCHAR(20) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    public_url VARCHAR(500) NOT NULL,
    width INT NULL,
    height INT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_media_asset_stored_name UNIQUE (stored_name),
    CONSTRAINT uk_media_asset_public_url UNIQUE (public_url),
    CONSTRAINT ck_media_asset_type CHECK (asset_type IN ('IMAGE','DOCUMENT')),
    CONSTRAINT ck_media_asset_size CHECK (size_bytes >= 0),
    CONSTRAINT ck_media_asset_dimensions CHECK (
        (width IS NULL OR width > 0) AND
        (height IS NULL OR height > 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 3. site_setting — singleton (id = 1)
-- ---------------------------------------------------------------
CREATE TABLE site_setting (
    id TINYINT NOT NULL,
    site_name VARCHAR(100) NOT NULL,
    tagline VARCHAR(255) NULL,
    site_url VARCHAR(255) NULL,
    logo_media_id BIGINT NULL,
    favicon_media_id BIGINT NULL,
    footer_text VARCHAR(500) NULL,
    github_url VARCHAR(500) NULL,
    default_seo_description VARCHAR(500) NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT ck_site_setting_singleton CHECK (id = 1),
    CONSTRAINT fk_site_setting_logo FOREIGN KEY (logo_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL,
    CONSTRAINT fk_site_setting_favicon FOREIGN KEY (favicon_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 4. tutorial_category — category tree (self FK, RESTRICT)
-- ---------------------------------------------------------------
CREATE TABLE tutorial_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_tutorial_category_slug UNIQUE (slug),
    KEY idx_tutorial_category_parent_sort (parent_id, sort_order, id),
    -- NOTE (approved spec change, TASK-002): frozen CHECK
    -- ck_tutorial_category_not_self (parent_id <> id) was dropped because MySQL
    -- 8.0.16+ forbids CHECK constraints referencing AUTO_INCREMENT columns.
    -- Self-parent prevention is a Service rule ("Category tree 不形成 cycle",
    -- 03-database-design.md §10) and is enforced there.
    CONSTRAINT fk_tutorial_category_parent FOREIGN KEY (parent_id)
        REFERENCES tutorial_category(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 5. tutorial — publish lifecycle DRAFT/PUBLISHED/WITHDRAWN
-- ---------------------------------------------------------------
CREATE TABLE tutorial (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    cover_media_id BIGINT NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    seo_title VARCHAR(200) NULL,
    seo_description VARCHAR(500) NULL,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_tutorial_slug UNIQUE (slug),
    KEY idx_tutorial_category_public_sort
        (category_id, publish_status, sort_order, id),
    CONSTRAINT ck_tutorial_publish_status CHECK (
        publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')
    ),
    CONSTRAINT ck_tutorial_publication_time CHECK (
        (publish_status='DRAFT' AND published_at IS NULL)
        OR
        (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)
    ),
    CONSTRAINT fk_tutorial_category FOREIGN KEY (category_id)
        REFERENCES tutorial_category(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tutorial_cover FOREIGN KEY (cover_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 6. tutorial_node — tree inside one tutorial (GROUP/CHAPTER).
--    CHAPTER is always a leaf; parent must belong to the SAME tutorial
--    (enforced by the composite FK on (tutorial_id, parent_id)).
-- ---------------------------------------------------------------
CREATE TABLE tutorial_node (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tutorial_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    node_type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NULL,
    summary VARCHAR(1000) NULL,
    body_markdown LONGTEXT NULL,
    publish_status VARCHAR(20) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_tutorial_node_tutorial_slug UNIQUE (tutorial_id, slug),
    CONSTRAINT uk_tutorial_node_tutorial_id_id UNIQUE (tutorial_id, id),
    KEY idx_tutorial_node_tree (tutorial_id, parent_id, sort_order, id),
    KEY idx_tutorial_node_public (tutorial_id, publish_status, sort_order, id),
    CONSTRAINT ck_tutorial_node_type CHECK (
        node_type IN ('GROUP','CHAPTER')
    ),
    CONSTRAINT ck_tutorial_node_shape CHECK (
        (
            node_type='GROUP'
            AND slug IS NULL
            AND body_markdown IS NULL
            AND publish_status IS NULL
            AND published_at IS NULL
        )
        OR
        (
            node_type='CHAPTER'
            AND slug IS NOT NULL
            AND body_markdown IS NOT NULL
            AND publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')
            AND (
                (publish_status='DRAFT' AND published_at IS NULL)
                OR
                (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)
            )
        )
    ),
    CONSTRAINT fk_tutorial_node_tutorial FOREIGN KEY (tutorial_id)
        REFERENCES tutorial(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tutorial_node_parent FOREIGN KEY (tutorial_id, parent_id)
        REFERENCES tutorial_node(tutorial_id, id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 7. blog_post — published_at = FIRST public publication time, immutable
-- ---------------------------------------------------------------
CREATE TABLE blog_post (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    body_markdown LONGTEXT NOT NULL,
    cover_media_id BIGINT NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    seo_title VARCHAR(200) NULL,
    seo_description VARCHAR(500) NULL,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_blog_post_slug UNIQUE (slug),
    KEY idx_blog_post_public_timeline (publish_status, published_at, id),
    CONSTRAINT ck_blog_post_publish_status CHECK (
        publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')
    ),
    CONSTRAINT ck_blog_post_publication_time CHECK (
        (publish_status='DRAFT' AND published_at IS NULL)
        OR
        (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)
    ),
    CONSTRAINT fk_blog_post_cover FOREIGN KEY (cover_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 8. blog_tag — flat tags
-- ---------------------------------------------------------------
CREATE TABLE blog_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(60) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_blog_tag_name UNIQUE (name),
    CONSTRAINT uk_blog_tag_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 9. blog_post_tag — Blog ↔ Tag relation (CASCADE both ways)
-- ---------------------------------------------------------------
CREATE TABLE blog_post_tag (
    blog_post_id BIGINT NOT NULL,
    blog_tag_id BIGINT NOT NULL,
    PRIMARY KEY (blog_post_id, blog_tag_id),
    KEY idx_blog_post_tag_tag (blog_tag_id, blog_post_id),
    CONSTRAINT fk_blog_post_tag_post FOREIGN KEY (blog_post_id)
        REFERENCES blog_post(id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_post_tag_tag FOREIGN KEY (blog_tag_id)
        REFERENCES blog_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 10. portfolio_project — publish_status and project_status are
--     INDEPENDENT states (never merged). ONLINE requires demo_url.
-- ---------------------------------------------------------------
CREATE TABLE portfolio_project (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    role VARCHAR(200) NULL,
    tech_stack JSON NOT NULL,
    body_markdown LONGTEXT NOT NULL,
    cover_media_id BIGINT NULL,
    repository_url VARCHAR(500) NULL,
    demo_url VARCHAR(500) NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    project_status VARCHAR(20) NOT NULL DEFAULT 'DEVELOPING',
    featured TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    started_at DATE NULL,
    completed_at DATE NULL,
    seo_title VARCHAR(200) NULL,
    seo_description VARCHAR(500) NULL,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_portfolio_project_slug UNIQUE (slug),
    KEY idx_portfolio_public_featured
        (publish_status, featured, sort_order, id),
    CONSTRAINT ck_portfolio_publish_status CHECK (
        publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')
    ),
    CONSTRAINT ck_portfolio_project_status CHECK (
        project_status IN ('DEVELOPING','COMPLETED','ONLINE')
    ),
    CONSTRAINT ck_portfolio_featured CHECK (featured IN (0,1)),
    CONSTRAINT ck_portfolio_publication_time CHECK (
        (publish_status='DRAFT' AND published_at IS NULL)
        OR
        (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)
    ),
    CONSTRAINT ck_portfolio_online_demo CHECK (
        project_status <> 'ONLINE' OR demo_url IS NOT NULL
    ),
    CONSTRAINT fk_portfolio_cover FOREIGN KEY (cover_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 11. profile — singleton (id = 1)
-- ---------------------------------------------------------------
CREATE TABLE profile (
    id TINYINT NOT NULL,
    display_name VARCHAR(100) NULL,
    headline VARCHAR(255) NULL,
    bio VARCHAR(1000) NULL,
    avatar_media_id BIGINT NULL,
    github_url VARCHAR(500) NULL,
    public_email VARCHAR(255) NULL,
    resume_media_id BIGINT NULL,
    current_focus JSON NOT NULL,
    technical_direction_markdown LONGTEXT NULL,
    journey_markdown LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT ck_profile_singleton CHECK (id = 1),
    CONSTRAINT fk_profile_avatar FOREIGN KEY (avatar_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL,
    CONSTRAINT fk_profile_resume FOREIGN KEY (resume_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 12. profile_selected_content — EXACTLY ONE business FK per row
-- ---------------------------------------------------------------
CREATE TABLE profile_selected_content (
    id BIGINT NOT NULL AUTO_INCREMENT,
    profile_id TINYINT NOT NULL,
    tutorial_id BIGINT NULL,
    blog_post_id BIGINT NULL,
    portfolio_project_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_profile_selected_sort (profile_id, sort_order, id),
    CONSTRAINT uk_profile_selected_tutorial UNIQUE (profile_id, tutorial_id),
    CONSTRAINT uk_profile_selected_blog UNIQUE (profile_id, blog_post_id),
    CONSTRAINT uk_profile_selected_portfolio UNIQUE (profile_id, portfolio_project_id),
    CONSTRAINT ck_profile_selected_exactly_one CHECK (
        (tutorial_id IS NOT NULL)
        + (blog_post_id IS NOT NULL)
        + (portfolio_project_id IS NOT NULL)
        = 1
    ),
    CONSTRAINT fk_profile_selected_profile FOREIGN KEY (profile_id)
        REFERENCES profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_profile_selected_tutorial FOREIGN KEY (tutorial_id)
        REFERENCES tutorial(id) ON DELETE CASCADE,
    CONSTRAINT fk_profile_selected_blog FOREIGN KEY (blog_post_id)
        REFERENCES blog_post(id) ON DELETE CASCADE,
    CONSTRAINT fk_profile_selected_portfolio FOREIGN KEY (portfolio_project_id)
        REFERENCES portfolio_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 13. english_overview — singleton (id = 1)
-- ---------------------------------------------------------------
CREATE TABLE english_overview (
    id TINYINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(500) NULL,
    introduction TEXT NULL,
    current_stage VARCHAR(50) NOT NULL,
    roadmap_markdown LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT ck_english_overview_singleton CHECK (id = 1),
    CONSTRAINT ck_english_current_stage CHECK (
        current_stage IN ('FOUNDATION','WORD_MEMORY','READING','ANALYTICS')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
