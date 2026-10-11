# 数据库迁移逐文件说明

本目录是 Flyway 的版本化迁移源。已有 SQL 中的表、字段、约束和业务原因注释继续有效；下面逐份补充职责、涉及对象及阅读提示。

**已发布的 40 份 SQL 保留原文。** Flyway 会校验已执行脚本，连注释变化也可能改变校验值，因此这次详细说明放在 README，不改历史脚本，也不要求部署修改数据库。README 不会被当作 SQL 执行。

关系以逻辑外键为主，数据库索引和 CHECK 约束见对应 SQL；跨模块关系和权限检查还需结合 Service 与 Mapper。迁移按版本执行，MySQL DDL 通常不能依靠应用事务完整回滚，结构变更应新增迁移，不能通过修改历史 SQL 修正。

## V2_001__account.sql

账号、凭证、固定角色与权限、管理员邀请、密码重置以及账号审计基础表；超级管理员由应用初始化，SQL 不写真实凭据。

- 创建：`sr_account`、`sr_account_credential`、`sr_role`、`sr_permission`、`sr_account_role`、`sr_role_permission`、`sr_admin_invitation`、`sr_password_reset`、`sr_account_audit`。
- 写入或修整记录：`sr_role`、`sr_permission`、`sr_role_permission`。
- 索引或约束：`uk_sr_account_username`、`uk_sr_account_email`、`idx_sr_account_status`、`idx_sr_account_created_at`、`uk_sr_credential_account`、`uk_sr_role_code`、`uk_sr_permission_code`、`idx_sr_permission_resource`、`uk_sr_account_role`、`idx_sr_account_role_role`、`uk_sr_role_permission`、`idx_sr_role_permission_permission`、`uk_sr_admin_invitation_token`、`idx_sr_admin_invitation_target_status`、`idx_sr_admin_invitation_expires_at`、`uk_sr_password_reset_token`、`idx_sr_password_reset_account_status`、`idx_sr_password_reset_expires_at`、`idx_sr_account_audit_actor`、`idx_sr_account_audit_target`、`idx_sr_account_audit_action`、`idx_sr_account_audit_created_at`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_001__account.sql](V2_001__account.sql)。

## V2_002__email_verification.sql

邮箱验证记录、验证用途与有效期，支撑注册、重置等邮件验证流程。

- 创建：`sr_email_verification`。
- 调整：`sr_account`。

读取源码：[V2_002__email_verification.sql](V2_002__email_verification.sql)。

## V2_003__fixed_account_policy_and_invitations.sql

收紧内置账户权限策略并完善邀请关系，角色权限不再依赖在线任意授权。

- 调整：`sr_admin_invitation`。
- 写入或修整记录：`sr_permission`、`sr_role_permission`、`sr_account`。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_003__fixed_account_policy_and_invitations.sql](V2_003__fixed_account_policy_and_invitations.sql)。

## V2_004__media.sql

媒体资产与业务引用关系，包括存储位置、访问级别、生命周期与引用归属。

- 创建：`sr_media_asset`、`sr_media_reference`。
- 写入或修整记录：`sr_permission`、`sr_role_permission`。
- 索引或约束：`uk_sr_media_storage_object`、`idx_sr_media_asset_sha256`、`idx_sr_media_asset_type_status`、`idx_sr_media_asset_access_status`、`idx_sr_media_asset_uploader_created`、`idx_sr_media_asset_created_at`、`uk_sr_media_reference_identity`、`idx_sr_media_reference_asset`、`idx_sr_media_reference_source`、`idx_sr_media_reference_source_usage`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_004__media.sql](V2_004__media.sql)。

## V2_005__media_types.sql

扩展媒体类型和类型校验，使图片、文档、音频、视频及压缩包具备对应元数据与限制。

- 调整：`sr_media_asset`。

读取源码：[V2_005__media_types.sql](V2_005__media_types.sql)。

## V2_006__review.sql

统一审批请求、审批动作与目标锁，支撑跨模块审核和重复提交控制。

- 创建：`sr_review_request`、`sr_review_action`。
- 写入或修整记录：`sr_permission`、`sr_role_permission`。
- 索引或约束：`idx_sr_review_status_submitted`、`idx_sr_review_target`、`idx_sr_review_target_type`、`idx_sr_review_applicant`、`idx_sr_review_reviewer`、`idx_sr_review_action_request`、`idx_sr_review_action_actor`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_006__review.sql](V2_006__review.sql)。

## V2_007__blog.sql

博客文章、标签、专题及有序专题成员关系；公开可见性由发布状态控制。

- 创建：`sr_blog_post`、`sr_blog_tag`、`sr_blog_post_tag`、`sr_blog_topic`、`sr_blog_topic_post`。
- 写入或修整记录：`sr_permission`、`sr_role_permission`。
- 索引或约束：`uk_sr_blog_post_slug`、`idx_sr_blog_post_status_published`、`idx_sr_blog_post_updated`、`uk_sr_blog_tag_slug`、`uk_sr_blog_tag_name`、`idx_sr_blog_tag_status`、`uk_sr_blog_post_tag`、`idx_sr_blog_post_tag_tag`、`uk_sr_blog_topic_slug`、`idx_sr_blog_topic_status`、`uk_sr_blog_topic_post`、`idx_sr_blog_topic_post_order`、`idx_sr_blog_topic_post_post`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_007__blog.sql](V2_007__blog.sql)。

## V2_008__blog_admin_editing.sql

博客后台编辑所需字段与约束，支持标题、正文及展示资料维护。

- 写入或修整记录：`sr_role_permission`。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_008__blog_admin_editing.sql](V2_008__blog_admin_editing.sql)。

## V2_009__tutorial_content.sql

教程、分组、章节与内容版本基础模型，教程管理员协作与公开发布的基础。

- 创建：`sr_tutorial_category`、`sr_tutorial`、`sr_tutorial_group`、`sr_tutorial_chapter`、`sr_tutorial_knowledge_card`、`sr_tutorial_question`、`sr_tutorial_revision`。
- 写入或修整记录：`sr_permission`、`sr_role_permission`。
- 索引或约束：`uk_sr_tutorial_category_slug`、`idx_sr_tutorial_category_order`、`uk_sr_tutorial_slug`、`idx_sr_tutorial_category`、`idx_sr_tutorial_publication`、`idx_sr_tutorial_editing`、`idx_sr_tutorial_published_revision`、`idx_sr_tutorial_group_tutorial`、`idx_sr_tutorial_group_sort`、`uk_sr_tutorial_chapter_slug`、`idx_sr_tutorial_chapter_group`、`idx_sr_tutorial_chapter_tutorial`、`idx_sr_tutorial_card_chapter`、`idx_sr_tutorial_question_chapter`、`uk_sr_tutorial_revision_no`、`uk_sr_tutorial_revision_ref`、`idx_sr_tutorial_revision_created`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_009__tutorial_content.sql](V2_009__tutorial_content.sql)。

## V2_010__tutorial_learning.sql

教程阅读进度、知识复习、学习计划等学习域基础数据。

- 创建：`sr_learning_progress`、`sr_user_question_answer`、`sr_study_plan`、`sr_study_task`、`sr_knowledge_mastery`、`sr_review_schedule`、`sr_review_task`、`sr_review_result`、`sr_learning_history`。
- 索引或约束：`uk_sr_learning_progress_account_chapter`、`idx_sr_learning_progress_recent`、`idx_sr_learning_progress_tutorial`、`uk_sr_user_question_answer`、`idx_sr_user_question_answer_account`、`idx_sr_user_question_answer_question`、`idx_sr_study_plan_account_status`、`idx_sr_study_plan_tutorial`、`uk_sr_study_task_identity`、`idx_sr_study_task_today`、`idx_sr_study_task_plan`、`uk_sr_mastery_account_card`、`idx_sr_mastery_account_level`、`uk_sr_review_schedule_account_card`、`idx_sr_review_schedule_due`、`idx_sr_review_schedule_account_due`、`uk_sr_review_task_schedule_due`、`idx_sr_review_task_account_due`、`idx_sr_review_task_card`、`uk_sr_review_result_task`、`idx_sr_review_result_account`、`idx_sr_review_result_card`、`idx_sr_learning_history_account_time`、`idx_sr_learning_history_account_tutorial`、`idx_sr_learning_history_event`；字段顺序与判断条件见脚本。

读取源码：[V2_010__tutorial_learning.sql](V2_010__tutorial_learning.sql)。

## V2_011__portfolio.sql

作品、链接、媒体及详情数据基础模型，作品发布与展示按模块权限管理。

- 创建：`sr_portfolio_work`、`sr_portfolio_work_detail`、`sr_portfolio_media`、`sr_portfolio_link`。
- 写入或修整记录：`sr_permission`、`sr_role_permission`。
- 索引或约束：`uk_sr_portfolio_work_slug`、`idx_sr_portfolio_work_type_status`、`idx_sr_portfolio_work_status`、`uk_sr_portfolio_work_detail`、`idx_sr_portfolio_work_detail_type`、`uk_sr_portfolio_media_identity`、`idx_sr_portfolio_media_order`、`idx_sr_portfolio_link_work`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_011__portfolio.sql](V2_011__portfolio.sql)。

## V2_012__message.sql

留言及管理动作，记录提交、审核和公开展示状态。

- 创建：`sr_message`、`sr_message_action`。
- 写入或修整记录：`sr_permission`、`sr_role_permission`。
- 索引或约束：`idx_sr_message_status_submitted`、`idx_sr_message_account`、`idx_sr_message_moderator`、`idx_sr_message_action_message`、`idx_sr_message_action_actor`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_012__message.sql](V2_012__message.sql)。

## V2_013__profile.sql

作者资料、经历、技能、社交链接与精选内容关联。

- 创建：`sr_profile`、`sr_profile_experience`、`sr_profile_skill`、`sr_profile_social_link`、`sr_profile_featured_content`。
- 写入或修整记录：`sr_profile`、`sr_permission`、`sr_role_permission`。
- 索引或约束：`uk_sr_profile_key`、`idx_sr_profile_experience`、`uk_sr_profile_skill`、`idx_sr_profile_skill_order`、`uk_sr_profile_social_platform`、`idx_sr_profile_social_order`、`uk_sr_profile_featured`、`idx_sr_profile_featured_order`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_013__profile.sql](V2_013__profile.sql)。

## V2_014__analytics.sql

前台访问事件与按日统计汇总，保留统计查询所需维度。

- 创建：`sr_analytics_event`、`sr_analytics_daily_site`、`sr_analytics_daily_content`、`sr_analytics_daily_referrer`。
- 写入或修整记录：`sr_permission`、`sr_role_permission`。
- 索引或约束：`idx_sr_analytics_event_time`、`idx_sr_analytics_event_type_time`、`idx_sr_analytics_event_content`、`idx_sr_analytics_event_referrer`、`idx_sr_analytics_content_hot`、`idx_sr_analytics_referrer_date`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_014__analytics.sql](V2_014__analytics.sql)。

## V2_015__search.sql

搜索文档投影和相关索引；搜索数据由业务内容派生，不替代正文源数据。

- 创建：`sr_search_document`。
- 写入或修整记录：`sr_permission`、`sr_role_permission`。
- 索引或约束：`uk_sr_search_document_key`、`uk_sr_search_content`、`idx_sr_search_type_status`、`idx_sr_search_status_time`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_015__search.sql](V2_015__search.sql)。

## V2_016__seo.sql

公开页面 SEO 投影与索引通知记录，维护地址、状态和更新信息。

- 创建：`sr_seo_page_snapshot`、`sr_seo_notification_log`。
- 写入或修整记录：`sr_permission`、`sr_role_permission`。
- 索引或约束：`uk_sr_seo_page_route`、`idx_sr_seo_page_status`、`idx_sr_seo_page_content`、`idx_sr_seo_notify_pending`、`idx_sr_seo_notify_route`、`idx_sr_seo_notify_provider`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_016__seo.sql](V2_016__seo.sql)。

## V2_017__site.sql

站点配置与首页内容区块基础数据。

- 创建：`sr_site_config`、`sr_site_home_section`。
- 写入或修整记录：`sr_site_config`、`sr_site_home_section`、`sr_permission`、`sr_role_permission`。
- 索引或约束：`uk_sr_site_config_key`、`uk_sr_site_home_section_code`、`idx_sr_site_home_section_order`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_017__site.sql](V2_017__site.sql)。

## V2_018__site_default_copy.sql

站点默认文案初始化，用可覆盖的配置数据组织公开说明。

- 写入或修整记录：`sr_site_config`。

读取源码：[V2_018__site_default_copy.sql](V2_018__site_default_copy.sql)。

## V2_019__english_overview.sql

英语模块概览、导航与学习统计基础结构。

- 创建：`sr_english_overview`。
- 写入或修整记录：`sr_english_overview`、`sr_permission`、`sr_role_permission`。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_019__english_overview.sql](V2_019__english_overview.sql)。

## V2_020__english_content.sql

英语内容基础结构，连接文档、词汇、语法、阅读、写作与原有听力模型。

- 创建：`sr_english_vocabulary_theme`、`sr_english_vocabulary_word`、`sr_english_grammar_course`、`sr_english_grammar_section`、`sr_english_grammar_lesson`、`sr_english_reading_article`、`sr_english_listening_item`、`sr_english_listening_segment`、`sr_english_writing_resource`、`sr_english_writing_prompt`。
- 写入或修整记录：`sr_english_grammar_course`、`sr_permission`、`sr_role_permission`。
- 索引或约束：`idx_english_theme_order`、`idx_english_word_theme`、`idx_english_word_lookup`、`idx_sr_english_grammar_section_order`、`uk_sr_english_grammar_lesson_slug`、`idx_sr_english_grammar_lesson_order`、`uk_sr_english_reading_slug`、`idx_sr_english_reading_public`、`uk_sr_english_listening_slug`、`idx_sr_english_listening_public`、`idx_sr_english_segment_order`、`uk_sr_english_writing_resource_slug`、`idx_sr_english_writing_resource_public`、`uk_sr_english_writing_prompt_slug`、`idx_sr_english_writing_prompt_public`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_020__english_content.sql](V2_020__english_content.sql)。

## V2_021__home_latest_and_site_copy.sql

首页最新内容与站点展示文案调整。

- 调整：`sr_site_config`。
- 写入或修整记录：`sr_site_config`、`sr_site_home_section`。

读取源码：[V2_021__home_latest_and_site_copy.sql](V2_021__home_latest_and_site_copy.sql)。

## V2_022__english_vocabulary_memory.sql

英语词汇记忆和学习记录，支持按账号累计复习证据。

- 创建：`sr_english_vocabulary_card_preference`、`sr_english_vocabulary_memory`、`sr_english_vocabulary_review_log`、`sr_english_vocabulary_study_setting`、`sr_english_vocabulary_word_audio`。
- 调整：`sr_english_vocabulary_word`。
- 索引或约束：`idx_sr_english_vocab_card_word`、`idx_sr_english_vocab_due`、`uk_sr_english_vocab_review_session`、`idx_sr_english_vocab_review_history`、`idx_sr_english_vocab_review_daily`、`uk_sr_english_vocab_audio_media`、`idx_sr_english_vocab_audio_primary`；字段顺序与判断条件见脚本。

读取源码：[V2_022__english_vocabulary_memory.sql](V2_022__english_vocabulary_memory.sql)。

## V2_023__remove_english_listening.sql

退役英语听力模块；包含实际删除操作，恢复旧功能不能仅回滚应用版本。

- 删除：`sr_english_listening_segment`、`sr_english_listening_item`。
- 写入或修整记录：`sr_media_reference`、`sr_permission`、`sr_english_overview`。
- 包含删除或退役操作：执行前核对对应数据备份；需要恢复时使用受控恢复或后续迁移。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_023__remove_english_listening.sql](V2_023__remove_english_listening.sql)。

## V2_024__blog_columns.sql

博客栏目和展示分类扩展。

- 写入或修整记录：`sr_blog_topic`。

读取源码：[V2_024__blog_columns.sql](V2_024__blog_columns.sql)。

## V2_025__blog_topic_navigation.sql

博客专题导航与相关展示字段调整。

- 调整：`sr_blog_topic`。
- 写入或修整记录：`sr_blog_topic`。
- 索引或约束：`idx_sr_blog_topic_navigation`；字段顺序与判断条件见脚本。

读取源码：[V2_025__blog_topic_navigation.sql](V2_025__blog_topic_navigation.sql)。

## V2_026__tutorial_chapter_lifecycle.sql

完善章节状态转换所需的生命周期字段和关联记录。

- 调整：`sr_tutorial_chapter`。
- 写入或修整记录：`sr_tutorial_chapter`、`sr_tutorial_group`、`sr_tutorial`。

读取源码：[V2_026__tutorial_chapter_lifecycle.sql](V2_026__tutorial_chapter_lifecycle.sql)。

## V2_027__tutorial_learning_evidence_model.sql

教程学习证据模型升级，将知识掌握、问题回答、复习和计划的证据归属明确化。

- 创建：`sr_tutorial_question_card`、`sr_learning_account_lock`、`sr_learning_session`、`sr_learning_session_item`、`sr_learning_evidence`、`sr_learning_legacy_review_map`。
- 调整：`sr_tutorial_knowledge_card`、`sr_knowledge_mastery`。
- 写入或修整记录：`sr_learning_session`、`sr_learning_legacy_review_map`、`sr_learning_evidence`、`sr_learning_session_item`、`sr_knowledge_mastery`。
- 索引或约束：`uk_question_card`、`idx_question_card_card`、`idx_session_current`、`idx_session_chapter`、`uk_session_card`、`idx_session_item_order`、`uk_evidence_session`、`uk_evidence_initial`、`idx_evidence_card`、`idx_evidence_account`、`idx_evidence_session`；字段顺序与判断条件见脚本。

读取源码：[V2_027__tutorial_learning_evidence_model.sql](V2_027__tutorial_learning_evidence_model.sql)。

## V2_028__tutorial_learning_answer_versions.sql

为学习回答建立版本信息，区分章节更新前后提交的内容。

- 创建：`sr_user_question_answer_version`。
- 调整：`sr_user_question_answer`。
- 写入或修整记录：`sr_user_question_answer_version`、`sr_user_question_answer`。
- 索引或约束：`uk_answer_version`；字段顺序与判断条件见脚本。

读取源码：[V2_028__tutorial_learning_answer_versions.sql](V2_028__tutorial_learning_answer_versions.sql)。

## V2_029__tutorial_learning_plan_tasks.sql

教程学习计划与任务模型扩展。

- 创建：`sr_study_plan_chapter`、`sr_study_task_chapter`、`sr_learning_legacy_completion`、`sr_learning_legacy_task_state`、`sr_learning_legacy_plan_state`。
- 调整：`sr_study_plan`、`sr_study_task`。
- 写入或修整记录：`sr_study_plan_chapter`、`sr_study_task_chapter`、`sr_study_task`、`sr_learning_progress`、`sr_study_plan`。
- 索引或约束：`uk_plan_chapter`、`idx_plan_chapter_conflict`、`uk_task_chapter`；字段顺序与判断条件见脚本。

读取源码：[V2_029__tutorial_learning_plan_tasks.sql](V2_029__tutorial_learning_plan_tasks.sql)。

## V2_030__tutorial_learning_legacy_orphan_evidence.sql

处理旧学习记录中的孤儿证据，保留或归档无法直接挂到当前对象的记录。

- 调整：`sr_learning_evidence`、`sr_learning_session`。
- 写入或修整记录：`sr_learning_session`、`sr_learning_legacy_review_map`、`sr_learning_evidence`、`sr_knowledge_mastery`。
- 索引或约束：`uk_session_legacy_result`；字段顺序与判断条件见脚本。

读取源码：[V2_030__tutorial_learning_legacy_orphan_evidence.sql](V2_030__tutorial_learning_legacy_orphan_evidence.sql)。

## V2_031__tutorial_repeat_study_rounds.sql

教程重复学习轮次，区分首次学习与后续重新学习。

- 创建：`sr_learning_session_question`。
- 调整：`sr_study_plan`、`sr_study_task`、`sr_learning_session`、`sr_study_plan_chapter`。
- 写入或修整记录：`sr_study_plan_chapter`、`sr_learning_session_question`。
- 索引或约束：`idx_study_task_round`、`idx_session_plan_round`；字段顺序与判断条件见脚本。

读取源码：[V2_031__tutorial_repeat_study_rounds.sql](V2_031__tutorial_repeat_study_rounds.sql)。

## V2_032__portfolio_works_sections.sql

作品分类与展示区块扩展，支持将作品说明组织成不同内容区块。

- 创建：`sr_portfolio_category`、`sr_portfolio_format`、`sr_portfolio_tag`、`sr_portfolio_work_tag`、`sr_portfolio_template`、`sr_portfolio_section`、`sr_portfolio_section_media`。
- 调整：`sr_portfolio_work`。
- 写入或修整记录：`sr_portfolio_category`、`sr_portfolio_format`、`sr_portfolio_tag`、`sr_portfolio_template`、`sr_portfolio_work`。
- 索引或约束：`idx_portfolio_category_status`、`idx_portfolio_format_status`、`idx_work_tag_tag`、`idx_work_section`、`uk_section_media`、`idx_section_asset`；字段顺序与判断条件见脚本。

读取源码：[V2_032__portfolio_works_sections.sql](V2_032__portfolio_works_sections.sql)。

## V2_033__portfolio_static_prototype.sql

作品静态原型关联模型，为原型资源和访问信息保留独立结构。

- 调整：`sr_portfolio_work`。
- 写入或修整记录：`sr_portfolio_work`。

读取源码：[V2_033__portfolio_static_prototype.sql](V2_033__portfolio_static_prototype.sql)。

## V2_034__vocabulary_intensive_learning.sql

词汇精学模型，将记忆、学习、测试和相关内容资料组织到词汇学习流程。

- 创建：`sr_english_vocabulary_mode_memory`、`sr_english_vocabulary_rating_day`、`sr_english_vocabulary_plan`、`sr_english_vocabulary_plan_item`。
- 调整：`sr_english_vocabulary_memory`、`sr_english_vocabulary_review_log`。
- 写入或修整记录：`sr_english_vocabulary_mode_memory`、`sr_english_vocabulary_study_setting`。
- 索引或约束：`idx_sr_vocab_rated_history`、`idx_sr_vocab_mode_due`、`chk_sr_vocab_mode_direction`、`uk_sr_vocab_plan_order`、`idx_sr_vocab_plan_group`、`chk_sr_vocab_done_mask`；字段顺序与判断条件见脚本。

读取源码：[V2_034__vocabulary_intensive_learning.sql](V2_034__vocabulary_intensive_learning.sql)。

## V2_035__english_knowledge_taxonomy.sql

英语知识分类树及分类关联，统一不同内容类型的知识组织方式。

- 创建：`sr_english_taxonomy_term`。
- 写入或修整记录：`sr_english_taxonomy_term`。
- 索引或约束：`uk_sr_english_taxonomy_slug`、`idx_sr_english_taxonomy_tree`、`idx_sr_english_taxonomy_baseline`；字段顺序与判断条件见脚本。

读取源码：[V2_035__english_knowledge_taxonomy.sql](V2_035__english_knowledge_taxonomy.sql)。

## V2_036__english_reading_writing_extensions.sql

英语阅读和写作扩展，包括增强阅读资料与写作内容关联。

- 创建：`sr_english_reading_tag`、`sr_english_reading_rights`、`sr_english_reading_alignment`、`sr_english_reading_annotation`、`sr_english_reading_vocabulary`、`sr_english_writing_article`、`sr_english_writing_article_tag`、`sr_english_reading_revision`、`sr_english_writing_revision`。
- 调整：`sr_english_reading_article`。
- 写入或修整记录：`sr_english_reading_rights`、`sr_permission`、`sr_role_permission`。
- 索引或约束：`idx_sr_reading_topic_public`、`idx_sr_english_reading_tag_term`、`idx_sr_english_reading_rights_state`、`idx_sr_reading_alignment_group`、`idx_sr_reading_alignment_paragraph`、`idx_sr_reading_annotation`、`idx_sr_reading_vocabulary`、`uk_sr_writing_article_slug`、`idx_sr_writing_owner_list`、`idx_sr_writing_public`、`idx_sr_writing_primary_topic`、`idx_sr_writing_tag_term`、`uk_sr_reading_revision`、`uk_sr_writing_revision`；字段顺序与判断条件见脚本。
- 包含权限数据：结合账户固定角色策略核对授予范围，不把权限初始化当成业务管理员创建。

读取源码：[V2_036__english_reading_writing_extensions.sql](V2_036__english_reading_writing_extensions.sql)。

## V2_037__retire_english_reading_levels_and_legacy_writing.sql

退役阅读等级和旧写作模型；包含结构或数据清理，需按实际 SQL 评估恢复策略。

- 调整：`sr_english_reading_article`。
- 删除：`sr_english_writing_resource`、`sr_english_writing_prompt`。
- 写入或修整记录：`sr_english_overview`。
- 包含删除或退役操作：执行前核对对应数据备份；需要恢复时使用受控恢复或后续迁移。

读取源码：[V2_037__retire_english_reading_levels_and_legacy_writing.sql](V2_037__retire_english_reading_levels_and_legacy_writing.sql)。

## V2_038__tutorial_workspace_versions.sql

教程工作区与版本模型，隔离草稿、提案和当前发布内容。

- 调整：`sr_tutorial_chapter`、`sr_tutorial_knowledge_card`、`sr_tutorial_question`。

读取源码：[V2_038__tutorial_workspace_versions.sql](V2_038__tutorial_workspace_versions.sql)。

## V2_039__tutorial_operation_review.sql

教程操作审核模型，保存操作提案、审核过程及执行状态。

- 创建：`sr_tutorial_operation`。
- 索引或约束：`uk_tutorial_operation_pending`、`idx_tutorial_operation_status`；字段顺序与判断条件见脚本。

读取源码：[V2_039__tutorial_operation_review.sql](V2_039__tutorial_operation_review.sql)。

## V2_040__tutorial_operation_failure_reason.sql

给教程操作增加 execution_message（VARCHAR(500)，可空），保存执行失败原因；保留原有提案、版本与审批决定。

- 调整：`sr_tutorial_operation`。

读取源码：[V2_040__tutorial_operation_failure_reason.sql](V2_040__tutorial_operation_failure_reason.sql)。
