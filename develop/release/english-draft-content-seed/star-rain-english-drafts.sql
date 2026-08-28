-- ============================================================================
-- 星雨笔录：英语听力、语音规则、写作与词族内容草稿包
-- 适用：MySQL 8.0 / 8.4，数据库 star_rain_notes，Flyway V25
-- 特性：仅新增缺失记录；不删除、不清空、不发布、不修改已有记录。
-- 注意：词族表没有 publish_status，词族写入后按现有系统逻辑立即可见。
-- ============================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION sql_safe_updates = 1;
START TRANSACTION;

-- --------------------------------------------------------------------------
-- 1. A1-C2 听力材料草稿
-- 仅完整音频和封面留空。听力稿、建议总时长及逐句时间片段均预先写入；
-- 上传真实音频后，可在后台按照实际波形微调时间边界。
-- --------------------------------------------------------------------------
SET @listening_base_order := (SELECT COALESCE(MAX(sort_order), 0) FROM english_listening_item);

INSERT INTO english_listening_item
    (title, slug, summary, transcript_markdown, cefr_level, listening_level,
     audio_media_id, cover_media_id, duration_seconds, source_name, source_url,
     copyright_note, publish_status, sort_order, published_at)
SELECT s.title, s.slug, s.summary, s.transcript_markdown, s.cefr_level, s.listening_level,
       NULL, NULL, s.duration_seconds, '星雨笔录原创', NULL, '星雨笔录原创学习内容，仅供本站学习使用。',
       'DRAFT', @listening_base_order + s.seq * 10, NULL
FROM (
    SELECT 1 seq, 'Meeting at the Library' title, '860828100101' slug,
           '两位朋友确认图书馆见面时间和当天安排，训练时间、地点与简单计划的听辨。' summary,
           'A1' cefr_level, 1 listening_level, 45 duration_seconds,
           '## Meeting at the Library

Mia: Hi, Leo. Are you going to the library today?

Leo: Yes. I want to read and return two books.

Mia: What time will you be there?

Leo: At ten o''clock. Can we meet near the front door?

Mia: Sure. I have a reading club at eleven.

Leo: Great. We can study first and have lunch after your club.

Mia: Good plan. Please bring your English notebook.

Leo: I will. See you at ten.

Mia: See you!

### 学习提示

第一次只听时间和地点，第二次记录两个人各自的安排，第三次跟读问句与回答。' transcript_markdown
    UNION ALL
    SELECT 2, 'Choosing the Right Train', '860828100102',
           '两位朋友比较周末车次、价格和集合时间，训练数字、时间和实用出行信息。',
           'A2', 1, 60,
           '## Choosing the Right Train

Maya: I found two trains to River Town on Friday evening.

Ben: What time do they leave?

Maya: The express train leaves at six ten. It takes one hour, but each ticket is eighty yuan.

Ben: And the other train?

Maya: It leaves at six forty and arrives at eight ten. The ticket is fifty-five yuan.

Ben: We finish work at five. The six-ten train may be difficult if traffic is busy.

Maya: I agree. The later train is safer, and we can buy dinner at the station.

Ben: How many tickets should I book?

Maya: Two. Please choose seats together if you can.

Ben: All right. Let''s meet by the main ticket gate at six fifteen.

Maya: Perfect. I will bring the hotel address and our weekend plan.

### 学习提示

建立车次比较表，分别记录出发、到达、票价和选择理由，再复述最终决定。'
    UNION ALL
    SELECT 3, 'A Student Changes His Phone Routine', '860828100103',
           '学生说明手机干扰问题，并通过观察触发因素建立可持续的专注习惯。',
           'B1', 2, 90,
           '## A Student Changes His Phone Routine

Last month, I realized that my phone was changing the way I studied. I often picked it up when an assignment became difficult. I told myself that I was taking a short break, but ten or fifteen minutes disappeared very quickly.

I spoke to our learning adviser. She did not tell me to delete every app. Instead, she asked me to notice when and why I checked my phone. For three days, I wrote down each unnecessary check. Most of them happened after a notification or when I felt unsure about the next step in my work.

We made a simple plan. I now turn off social notifications during study time and put the phone inside my bag. I work for twenty-five minutes before I take a planned break. If I feel stuck, I write the problem on paper instead of immediately opening an app.

The plan is not perfect, but it has changed my attention. I still use my phone for class messages and useful tools. The difference is that I choose when to use it. At the end of the week, I review what worked and make one small change for the next week.

### 学习提示

区分问题、触发因素、措施和复盘四层信息，注意 instead、if 和 but 如何连接逻辑。'
    UNION ALL
    SELECT 4, 'A Remote Team Reviews Its Communication Rules', '860828100104',
           '团队会议讨论响应期限、紧急频道、决策记录和会议用途，训练建议与行动项理解。',
           'B2', 2, 120,
           '## A Remote Team Reviews Its Communication Rules

Nora: Before we plan the next project, I want to review how we communicate. Several people said they were interrupted by messages that looked urgent but were not.

Evan: I agree. Yesterday I stopped a difficult task three times, and two messages could easily have waited until Friday.

Priya: Could we add a response label? “Today” could mean answer before the end of your working day, while “this week” would give people more space.

Nora: That sounds useful, but we also need a channel for genuine emergencies. If the website is unavailable, nobody should wait several hours for a reply.

Evan: Let''s keep one alert channel for issues that affect customers immediately. Everything else should go into the project channel with a clear response label.

Priya: I also want written decision notes. Last week we changed the release date in a call, but the design board still showed the old date.

Nora: Good point. After any decision meeting, the organizer will record the decision, the reason, the owner, and the review date.

Evan: Does that mean more paperwork after every conversation?

Nora: Only after a decision. Informal discussion can remain informal, but a decision must have one reliable home.

Priya: What about weekly status meetings? Mine often repeat information that is already in our task board.

Evan: We could post status updates in writing and use meeting time for choices, creative discussion, or problems where tone matters.

Nora: Let''s try these rules for two weeks. Priya will create the response labels, Evan will update the decision-note template, and I will shorten the weekly meeting. We will review the results next Friday.

### 学习提示

听第一遍识别问题，第二遍按讨论顺序记录四项规则，第三遍确认负责人和复盘时间。'
    UNION ALL
    SELECT 5, 'A Lecture on Responsible AI Support', '860828100105',
           '教师培训讲座区分辅助、判断和责任，训练论证结构、限定条件及说话者立场。',
           'C1', 3, 150,
           '## A Lecture on Responsible AI Support

When schools discuss artificial intelligence, the conversation often becomes a choice between enthusiasm and prohibition. That choice is too simple. The educational value of a tool depends not only on what it can produce, but also on the role it is allowed to play.

Consider feedback on student writing. An AI system can identify repeated sentence patterns, suggest alternative transitions, and provide another explanation within seconds. This can give learners more opportunities to revise. Yet speed does not guarantee suitability. A fluent suggestion may be inaccurate, or it may remove a feature that expresses the student''s intended voice.

For that reason, the goal should not be automatic correction. The goal should be supported judgment. A learner might compare two revisions, explain which one better communicates the intended meaning, and verify an uncertain claim with a trusted source. The tool supplies possibilities; the learner remains responsible for the choice.

The distinction matters even more in high-stakes decisions. Final grades, findings of misconduct, and decisions about learning support affect a student''s future. A system may help organize evidence, but a qualified person must interpret the context, explain the decision, and provide a way to challenge it. Human review is meaningful only when the reviewer has enough time and authority to disagree.

Responsible use also requires transparency at the classroom level. Students need to know which forms of assistance are permitted and why. If the purpose of an assignment is to practice planning an argument, asking a tool for possible counterarguments may support that goal. Asking it to produce the complete final essay would replace the skill under assessment.

The most useful policy is therefore specific rather than dramatic. Define the learning objective, decide which assistance serves it, require verification where errors matter, and keep consequential judgment with accountable people. Artificial intelligence can extend practice and access, but it should not quietly become the author, examiner, and final authority at the same time.

### 学习提示

为每段写一句功能标签：提出框架、承认收益、设定目标、高风险边界、透明规则、总结政策。'
    UNION ALL
    SELECT 6, 'Public Briefing on an Algorithmic Grant System', '860828100106',
           '公共听证会讨论算法建议、申诉权、整体监测和实质人工复核之间的责任关系。',
           'C2', 3, 180,
           '## Public Briefing on an Algorithmic Grant System

Moderator: We are examining the city''s proposed system for prioritizing home-energy grants. The system will produce a score, but officials have repeatedly called that score advisory. Dr. Chen, why are residents still concerned?

Dr. Chen: Because a recommendation can function as a decision even when policy uses a different word. If staff follow the score in nearly every case, applicants experience the algorithm as an authority. We must evaluate operational practice, not merely the label in the contract.

Moderator: The city says every case will receive human review. Does that answer the concern?

Dr. Chen: Only if the review is substantive. A reviewer needs access to the relevant information, enough time to examine it, and genuine authority to disagree. If rejecting the score creates excessive work or harms the reviewer''s performance record, the process encourages automatic approval.

Resident: Suppose the information about my household is wrong. What should I be able to do?

Dr. Chen: You should receive a case-specific explanation, a practical way to correct the data, and an appeal reviewed by someone who was not responsible for the original outcome. An appeal that requires technical expertise or months of effort is not meaningfully accessible.

City Official: We can provide explanations, but publishing every technical detail may create security and privacy risks.

Dr. Chen: Full public access to every line of code is not the only form of transparency. Different audiences need different explanations. Auditors need technical access. Applicants need to know what affected their case. Policy makers and the public need evidence that the system advances the stated goal without imposing unjustified burdens.

Moderator: Is individual appeal enough?

Dr. Chen: No. Appeals can correct particular errors, but the city must also monitor patterns across many decisions. A rule may appear neutral in one case while consistently disadvantaging a group. Aggregate review asks whether the policy itself should change.

Resident: Then who is finally responsible: the developer, the reviewer, or the city?

Dr. Chen: The city cannot outsource public responsibility. Developers are responsible for professional work and honest disclosure. Reviewers are responsible for exercising judgment. But the public body chooses the purpose, adopts the process, and acts on its results. It must name an accountable owner and preserve democratic oversight.

Moderator: So your position is not that the city must reject the system.

Dr. Chen: Correct. An algorithm may help organize information and identify cases that deserve attention. The principle is that influence must be matched by accountability. As the system''s influence grows, duties of explanation, monitoring, review, and appeal must grow with it.

### 学习提示

区分“名义人工复核”和“实质人工复核”，再归纳个案申诉、整体监测与最终责任三条线。'
) AS s
WHERE NOT EXISTS (SELECT 1 FROM english_listening_item x WHERE x.slug = s.slug);

-- 若曾执行过未含时间片段的早期草稿包，仅补正本包记录的建议时长，不触碰其他内容。
UPDATE english_listening_item
SET duration_seconds = CASE slug
    WHEN '860828100101' THEN 45 WHEN '860828100102' THEN 60
    WHEN '860828100103' THEN 90 WHEN '860828100104' THEN 120
    WHEN '860828100105' THEN 150 WHEN '860828100106' THEN 180
    ELSE duration_seconds END
WHERE slug BETWEEN '860828100101' AND '860828100106'
  AND audio_media_id IS NULL
  AND duration_seconds = 0;

-- 听力标签：按标签 slug 关联，不依赖固定主键。
INSERT IGNORE INTO english_listening_item_tag (listening_item_id, term_id, tag_role)
SELECT i.id, t.id, 'TAG'
FROM english_listening_item i
JOIN (
    SELECT '860828100101' item_slug, 'topic-culture' term_slug UNION ALL
    SELECT '860828100101', 'topic-society' UNION ALL SELECT '860828100101', 'scene-daily' UNION ALL
    SELECT '860828100101', 'format-short-dialogue' UNION ALL SELECT '860828100101', 'ability-phoneme' UNION ALL
    SELECT '860828100101', 'ability-extract' UNION ALL SELECT '860828100101', 'function-describe' UNION ALL
    SELECT '860828100102', 'topic-culture' UNION ALL SELECT '860828100102', 'scene-daily' UNION ALL
    SELECT '860828100102', 'format-long-dialogue' UNION ALL SELECT '860828100102', 'ability-extract' UNION ALL
    SELECT '860828100102', 'ability-retention' UNION ALL SELECT '860828100102', 'function-compare' UNION ALL
    SELECT '860828100102', 'function-summarize' UNION ALL
    SELECT '860828100103', 'topic-health' UNION ALL SELECT '860828100103', 'topic-tech' UNION ALL
    SELECT '860828100103', 'scene-study' UNION ALL SELECT '860828100103', 'format-monologue' UNION ALL
    SELECT '860828100103', 'ability-extract' UNION ALL SELECT '860828100103', 'ability-retention' UNION ALL
    SELECT '860828100103', 'function-cause' UNION ALL SELECT '860828100103', 'function-explain' UNION ALL
    SELECT '860828100104', 'topic-economy' UNION ALL SELECT '860828100104', 'topic-society' UNION ALL
    SELECT '860828100104', 'scene-work' UNION ALL SELECT '860828100104', 'format-long-dialogue' UNION ALL
    SELECT '860828100104', 'ability-extract' UNION ALL SELECT '860828100104', 'ability-logic' UNION ALL
    SELECT '860828100104', 'ability-retention' UNION ALL SELECT '860828100104', 'function-compare' UNION ALL
    SELECT '860828100104', 'function-cause' UNION ALL SELECT '860828100104', 'function-summarize' UNION ALL
    SELECT '860828100105', 'topic-tech' UNION ALL SELECT '860828100105', 'topic-society' UNION ALL
    SELECT '860828100105', 'scene-study' UNION ALL SELECT '860828100105', 'format-lecture' UNION ALL
    SELECT '860828100105', 'ability-logic' UNION ALL SELECT '860828100105', 'ability-inference' UNION ALL
    SELECT '860828100105', 'ability-retention' UNION ALL SELECT '860828100105', 'function-argue' UNION ALL
    SELECT '860828100105', 'function-contrast' UNION ALL SELECT '860828100105', 'function-summarize' UNION ALL
    SELECT '860828100106', 'topic-politics-policy' UNION ALL SELECT '860828100106', 'topic-philosophy-value' UNION ALL
    SELECT '860828100106', 'topic-tech' UNION ALL SELECT '860828100106', 'scene-news' UNION ALL
    SELECT '860828100106', 'format-news' UNION ALL SELECT '860828100106', 'ability-inference' UNION ALL
    SELECT '860828100106', 'ability-logic' UNION ALL SELECT '860828100106', 'ability-retention' UNION ALL
    SELECT '860828100106', 'function-explain' UNION ALL SELECT '860828100106', 'function-argue' UNION ALL
    SELECT '860828100106', 'function-rebut' UNION ALL SELECT '860828100106', 'function-summarize'
) m ON m.item_slug = i.slug
JOIN english_taxonomy_term t ON t.slug = m.term_slug AND t.enabled = 1;

-- 听力时间片段：按对话轮次或完整语义句群划分，毫秒区间不重叠。
-- 这些时间是配合建议语速的初始值；上传真实音频后应试听并微调。
INSERT INTO english_listening_segment
    (listening_item_id, start_ms, end_ms, transcript_text, translation_text, sort_order)
SELECT i.id, s.start_ms, s.end_ms, s.transcript_text, s.translation_text, s.sort_order
FROM english_listening_item i
JOIN (
    SELECT '860828100101' item_slug, 0 start_ms, 3600 end_ms,
           'Mia: Hi, Leo. Are you going to the library today?' transcript_text,
           '米娅：嗨，利奥。你今天要去图书馆吗？' translation_text, 10 sort_order
    UNION ALL SELECT '860828100101', 3800, 7100, 'Leo: Yes. I want to read and return two books.', '利奥：是的。我想读书，还要归还两本书。', 20
    UNION ALL SELECT '860828100101', 7300, 10100, 'Mia: What time will you be there?', '米娅：你几点会到那里？', 30
    UNION ALL SELECT '860828100101', 10300, 14500, 'Leo: At ten o''clock. Can we meet near the front door?', '利奥：十点。我们能在正门附近见面吗？', 40
    UNION ALL SELECT '860828100101', 14700, 17800, 'Mia: Sure. I have a reading club at eleven.', '米娅：当然。我十一点有读书会。', 50
    UNION ALL SELECT '860828100101', 18000, 23100, 'Leo: Great. We can study first and have lunch after your club.', '利奥：太好了。我们可以先学习，等你的读书会结束后再吃午饭。', 60
    UNION ALL SELECT '860828100101', 23300, 27100, 'Mia: Good plan. Please bring your English notebook.', '米娅：好计划。请带上你的英语笔记本。', 70
    UNION ALL SELECT '860828100101', 27300, 30700, 'Leo: I will. See you at ten.', '利奥：我会的。十点见。', 80
    UNION ALL SELECT '860828100101', 30900, 32900, 'Mia: See you!', '米娅：再见！', 90

    UNION ALL SELECT '860828100102', 0, 4200, 'Maya: I found two trains to River Town on Friday evening.', '玛雅：我找到了周五晚上去河畔镇的两趟火车。', 10
    UNION ALL SELECT '860828100102', 4400, 6700, 'Ben: What time do they leave?', '本：它们几点出发？', 20
    UNION ALL SELECT '860828100102', 6900, 12800, 'Maya: The express train leaves at six ten. It takes one hour, but each ticket is eighty yuan.', '玛雅：快车六点十分出发，车程一小时，但每张票八十元。', 30
    UNION ALL SELECT '860828100102', 13000, 15100, 'Ben: And the other train?', '本：另一趟呢？', 40
    UNION ALL SELECT '860828100102', 15300, 20100, 'Maya: It leaves at six forty and arrives at eight ten. The ticket is fifty-five yuan.', '玛雅：它六点四十分出发，八点十分到达，票价五十五元。', 50
    UNION ALL SELECT '860828100102', 20300, 25900, 'Ben: We finish work at five. The six-ten train may be difficult if traffic is busy.', '本：我们五点下班。如果交通拥堵，赶六点十分的车可能有困难。', 60
    UNION ALL SELECT '860828100102', 26100, 30900, 'Maya: I agree. The later train is safer, and we can buy dinner at the station.', '玛雅：我同意。晚一点的车更稳妥，我们还可以在车站买晚餐。', 70
    UNION ALL SELECT '860828100102', 31100, 34000, 'Ben: How many tickets should I book?', '本：我应该订几张票？', 80
    UNION ALL SELECT '860828100102', 34200, 38100, 'Maya: Two. Please choose seats together if you can.', '玛雅：两张。如果可以，请选相邻的座位。', 90
    UNION ALL SELECT '860828100102', 38300, 42900, 'Ben: All right. Let''s meet by the main ticket gate at six fifteen.', '本：好的。我们六点十五分在主检票口旁见。', 100
    UNION ALL SELECT '860828100102', 43100, 48000, 'Maya: Perfect. I will bring the hotel address and our weekend plan.', '玛雅：太好了。我会带上酒店地址和周末计划。', 110

    UNION ALL SELECT '860828100103', 0, 7600, 'Last month, I realized that my phone was changing the way I studied.', '上个月，我意识到手机正在改变我的学习方式。', 10
    UNION ALL SELECT '860828100103', 7800, 16300, 'I often picked it up when an assignment became difficult. I told myself that I was taking a short break, but ten or fifteen minutes disappeared very quickly.', '作业一变难，我就经常拿起手机。我告诉自己只是短暂休息，但十到十五分钟很快就过去了。', 20
    UNION ALL SELECT '860828100103', 16500, 22500, 'I spoke to our learning adviser. She did not tell me to delete every app.', '我和学习顾问谈了谈。她没有让我删除所有应用。', 30
    UNION ALL SELECT '860828100103', 22700, 32900, 'Instead, she asked me to notice when and why I checked my phone. For three days, I wrote down each unnecessary check.', '相反，她让我观察自己何时以及为何查看手机。我连续三天记录了每一次不必要的查看。', 40
    UNION ALL SELECT '860828100103', 33100, 39600, 'Most of them happened after a notification or when I felt unsure about the next step in my work.', '大多数情况发生在通知出现后，或者我不确定下一步该做什么的时候。', 50
    UNION ALL SELECT '860828100103', 39800, 47600, 'We made a simple plan. I now turn off social notifications during study time and put the phone inside my bag.', '我们制定了一个简单计划。现在学习时我会关闭社交通知，并把手机放进包里。', 60
    UNION ALL SELECT '860828100103', 47800, 57300, 'I work for twenty-five minutes before I take a planned break. If I feel stuck, I write the problem on paper instead of immediately opening an app.', '我先学习二十五分钟，再按计划休息。如果遇到困难，我会把问题写在纸上，而不是立刻打开应用。', 70
    UNION ALL SELECT '860828100103', 57500, 65000, 'The plan is not perfect, but it has changed my attention. I still use my phone for class messages and useful tools.', '这个计划并不完美，但它改变了我的注意力。我仍会用手机查看课程消息和使用实用工具。', 80
    UNION ALL SELECT '860828100103', 65200, 75500, 'The difference is that I choose when to use it. At the end of the week, I review what worked and make one small change for the next week.', '不同之处在于，我会主动选择何时使用手机。每周结束时，我会复盘有效的方法，并为下一周做一个小调整。', 90

    UNION ALL SELECT '860828100104', 0, 7600, 'Nora: Before we plan the next project, I want to review how we communicate. Several people said they were interrupted by messages that looked urgent but were not.', '诺拉：在规划下一个项目之前，我想复盘我们的沟通方式。几位同事说，他们被看似紧急、实际并不紧急的消息打断了。', 10
    UNION ALL SELECT '860828100104', 7800, 13900, 'Evan: I agree. Yesterday I stopped a difficult task three times, and two messages could easily have waited until Friday.', '埃文：我同意。昨天我三次停下困难任务，其中两条消息完全可以等到周五。', 20
    UNION ALL SELECT '860828100104', 14100, 22400, 'Priya: Could we add a response label? “Today” could mean answer before the end of your working day, while “this week” would give people more space.', '普里娅：我们能否增加响应标签？“今天”表示下班前回复，“本周”则给大家更多时间。', 30
    UNION ALL SELECT '860828100104', 22600, 28600, 'Nora: That sounds useful, but we also need a channel for genuine emergencies. If the website is unavailable, nobody should wait several hours for a reply.', '诺拉：这很有用，但我们还需要一个处理真正紧急情况的频道。如果网站无法访问，就不该等几个小时才回复。', 40
    UNION ALL SELECT '860828100104', 28800, 36200, 'Evan: Let''s keep one alert channel for issues that affect customers immediately. Everything else should go into the project channel with a clear response label.', '埃文：保留一个警报频道处理立刻影响客户的问题。其他内容都进入项目频道，并标注明确的响应期限。', 50
    UNION ALL SELECT '860828100104', 36400, 42100, 'Priya: I also want written decision notes. Last week we changed the release date in a call, but the design board still showed the old date.', '普里娅：我还希望有书面决策记录。上周我们在电话中改了发布日期，但设计看板仍显示旧日期。', 60
    UNION ALL SELECT '860828100104', 42300, 48600, 'Nora: Good point. After any decision meeting, the organizer will record the decision, the reason, the owner, and the review date.', '诺拉：说得好。每次决策会议后，组织者都要记录决定、理由、负责人和复盘日期。', 70
    UNION ALL SELECT '860828100104', 48800, 52700, 'Evan: Does that mean more paperwork after every conversation?', '埃文：这是否意味着每次谈话后都要增加文书工作？', 80
    UNION ALL SELECT '860828100104', 52900, 59400, 'Nora: Only after a decision. Informal discussion can remain informal, but a decision must have one reliable home.', '诺拉：只在作出决定后。非正式讨论可以保持非正式，但决定必须有一个可靠的记录位置。', 90
    UNION ALL SELECT '860828100104', 59600, 65900, 'Priya: What about weekly status meetings? Mine often repeat information that is already in our task board.', '普里娅：每周状态会议呢？我的会议经常重复任务看板里已经有的信息。', 100
    UNION ALL SELECT '860828100104', 66100, 73200, 'Evan: We could post status updates in writing and use meeting time for choices, creative discussion, or problems where tone matters.', '埃文：我们可以书面发布状态更新，把会议时间用于决策、创意讨论或需要语气交流的问题。', 110
    UNION ALL SELECT '860828100104', 73400, 82800, 'Nora: Let''s try these rules for two weeks. Priya will create the response labels, Evan will update the decision-note template, and I will shorten the weekly meeting.', '诺拉：我们试行两周。普里娅创建响应标签，埃文更新决策记录模板，我来缩短周会。', 120
    UNION ALL SELECT '860828100104', 83000, 86900, 'Nora: We will review the results next Friday.', '诺拉：我们将在下周五复盘结果。', 130

    UNION ALL SELECT '860828100105', 0, 12700, 'When schools discuss artificial intelligence, the conversation often becomes a choice between enthusiasm and prohibition. That choice is too simple. The educational value of a tool depends not only on what it can produce, but also on the role it is allowed to play.', '学校讨论人工智能时，话题常被简化为热情支持或全面禁止的二选一。这过于简单。工具的教育价值不仅取决于它能生成什么，也取决于它被允许扮演什么角色。', 10
    UNION ALL SELECT '860828100105', 12900, 25400, 'Consider feedback on student writing. An AI system can identify repeated sentence patterns, suggest alternative transitions, and provide another explanation within seconds. This can give learners more opportunities to revise.', '以学生写作反馈为例。人工智能可以识别重复句式、建议其他衔接方式，并迅速提供另一种解释，从而给学习者更多修改机会。', 20
    UNION ALL SELECT '860828100105', 25600, 33700, 'Yet speed does not guarantee suitability. A fluent suggestion may be inaccurate, or it may remove a feature that expresses the student''s intended voice.', '然而，速度并不保证适用性。流畅的建议可能不准确，也可能删掉体现学生本意和个人表达的特征。', 30
    UNION ALL SELECT '860828100105', 33900, 45500, 'For that reason, the goal should not be automatic correction. The goal should be supported judgment. A learner might compare two revisions, explain which one better communicates the intended meaning, and verify an uncertain claim with a trusted source.', '因此，目标不应是自动纠错，而应是辅助判断。学习者可以比较两个修改版本，说明哪个更准确地表达原意，并用可信来源核实不确定的论断。', 40
    UNION ALL SELECT '860828100105', 45700, 49300, 'The tool supplies possibilities; the learner remains responsible for the choice.', '工具提供可能性，学习者仍对选择负责。', 50
    UNION ALL SELECT '860828100105', 49500, 63200, 'The distinction matters even more in high-stakes decisions. Final grades, findings of misconduct, and decisions about learning support affect a student''s future. A system may help organize evidence, but a qualified person must interpret the context, explain the decision, and provide a way to challenge it.', '这种区分在高风险决策中更重要。最终成绩、学术不端认定和学习支持决定都会影响学生未来。系统可以帮助整理证据，但必须由合格人员解释情境和决定，并提供申诉渠道。', 60
    UNION ALL SELECT '860828100105', 63400, 71600, 'Human review is meaningful only when the reviewer has enough time and authority to disagree.', '只有当复核者拥有足够时间和否决权限时，人工复核才有实质意义。', 70
    UNION ALL SELECT '860828100105', 71800, 84500, 'Responsible use also requires transparency at the classroom level. Students need to know which forms of assistance are permitted and why. If the purpose of an assignment is to practice planning an argument, asking a tool for possible counterarguments may support that goal.', '负责任的使用还需要课堂层面的透明。学生需要知道允许哪些辅助以及原因。如果作业目标是练习论证规划，让工具提供可能的反方观点可以服务这一目标。', 80
    UNION ALL SELECT '860828100105', 84700, 92900, 'Asking it to produce the complete final essay would replace the skill under assessment.', '但让工具生成完整终稿，就会取代本应接受评估的能力。', 90
    UNION ALL SELECT '860828100105', 93100, 108000, 'The most useful policy is therefore specific rather than dramatic. Define the learning objective, decide which assistance serves it, require verification where errors matter, and keep consequential judgment with accountable people.', '因此，最有用的政策应具体而非夸张：明确学习目标，判断哪些辅助真正服务目标，在错误影响重大的地方要求核验，并把关键判断交给可问责的人。', 100
    UNION ALL SELECT '860828100105', 108200, 119500, 'Artificial intelligence can extend practice and access, but it should not quietly become the author, examiner, and final authority at the same time.', '人工智能可以拓展练习机会和可及性，但不应悄然同时成为作者、考官和最终裁决者。', 110

    UNION ALL SELECT '860828100106', 0, 11600, 'Moderator: We are examining the city''s proposed system for prioritizing home-energy grants. The system will produce a score, but officials have repeatedly called that score advisory. Dr. Chen, why are residents still concerned?', '主持人：我们正在审查该市拟议的家庭节能补助排序系统。系统会生成分数，但官员反复强调该分数仅供参考。陈博士，居民为什么仍然担忧？', 10
    UNION ALL SELECT '860828100106', 11800, 22700, 'Dr. Chen: Because a recommendation can function as a decision even when policy uses a different word. If staff follow the score in nearly every case, applicants experience the algorithm as an authority. We must evaluate operational practice, not merely the label in the contract.', '陈博士：即使政策使用不同措辞，建议也可能实际发挥决定作用。如果工作人员几乎总是服从评分，申请者就会把算法体验为权威。我们必须评估实际运行，而非只看合同标签。', 20
    UNION ALL SELECT '860828100106', 22900, 26700, 'Moderator: The city says every case will receive human review. Does that answer the concern?', '主持人：市政府称每个案件都会接受人工复核。这能消除担忧吗？', 30
    UNION ALL SELECT '860828100106', 26900, 38100, 'Dr. Chen: Only if the review is substantive. A reviewer needs access to the relevant information, enough time to examine it, and genuine authority to disagree. If rejecting the score creates excessive work or harms the reviewer''s performance record, the process encourages automatic approval.', '陈博士：只有复核具有实质性才可以。复核者需要获得相关信息、有足够时间审查并拥有真正的否决权限。如果拒绝评分会增加大量工作或损害绩效，流程就会鼓励自动批准。', 40
    UNION ALL SELECT '860828100106', 38300, 42100, 'Resident: Suppose the information about my household is wrong. What should I be able to do?', '居民：假如系统掌握的家庭信息有误，我应该能够做什么？', 50
    UNION ALL SELECT '860828100106', 42300, 52300, 'Dr. Chen: You should receive a case-specific explanation, a practical way to correct the data, and an appeal reviewed by someone who was not responsible for the original outcome. An appeal that requires technical expertise or months of effort is not meaningfully accessible.', '陈博士：你应获得针对个案的解释、可行的数据纠正方式，以及由未参与原决定的人审查的申诉渠道。需要专业技术或数月精力的申诉并非真正可及。', 60
    UNION ALL SELECT '860828100106', 52500, 58000, 'City Official: We can provide explanations, but publishing every technical detail may create security and privacy risks.', '市政府官员：我们可以提供解释，但公布所有技术细节可能带来安全和隐私风险。', 70
    UNION ALL SELECT '860828100106', 58200, 70400, 'Dr. Chen: Full public access to every line of code is not the only form of transparency. Different audiences need different explanations. Auditors need technical access. Applicants need to know what affected their case.', '陈博士：向公众开放每一行代码并不是透明的唯一形式。不同受众需要不同解释。审计人员需要技术访问，申请者需要知道哪些因素影响了自己的案件。', 80
    UNION ALL SELECT '860828100106', 70600, 76500, 'Dr. Chen: Policy makers and the public need evidence that the system advances the stated goal without imposing unjustified burdens.', '陈博士：政策制定者和公众需要证据，确认系统在推进既定目标时没有造成不合理负担。', 90
    UNION ALL SELECT '860828100106', 76700, 79000, 'Moderator: Is individual appeal enough?', '主持人：个案申诉足够吗？', 100
    UNION ALL SELECT '860828100106', 79200, 89500, 'Dr. Chen: No. Appeals can correct particular errors, but the city must also monitor patterns across many decisions. A rule may appear neutral in one case while consistently disadvantaging a group.', '陈博士：不够。申诉可以纠正个别错误，但市政府还必须监测大量决定中的整体模式。一条规则在个案中可能看似中立，却持续让某个群体处于不利地位。', 110
    UNION ALL SELECT '860828100106', 89700, 94100, 'Dr. Chen: Aggregate review asks whether the policy itself should change.', '陈博士：整体审查追问的是政策本身是否需要改变。', 120
    UNION ALL SELECT '860828100106', 94300, 98700, 'Resident: Then who is finally responsible: the developer, the reviewer, or the city?', '居民：那么最终由谁负责：开发者、复核者，还是市政府？', 130
    UNION ALL SELECT '860828100106', 98900, 111500, 'Dr. Chen: The city cannot outsource public responsibility. Developers are responsible for professional work and honest disclosure. Reviewers are responsible for exercising judgment. But the public body chooses the purpose, adopts the process, and acts on its results.', '陈博士：市政府不能外包公共责任。开发者对专业工作和诚实披露负责，复核者对运用判断负责，但公共机构选择目标、采用流程并根据结果采取行动。', 140
    UNION ALL SELECT '860828100106', 111700, 117100, 'Dr. Chen: It must name an accountable owner and preserve democratic oversight.', '陈博士：它必须指定可问责的负责人，并保留民主监督。', 150
    UNION ALL SELECT '860828100106', 117300, 121400, 'Moderator: So your position is not that the city must reject the system.', '主持人：所以你的立场并不是市政府必须拒绝这个系统。', 160
    UNION ALL SELECT '860828100106', 121600, 133900, 'Dr. Chen: Correct. An algorithm may help organize information and identify cases that deserve attention. The principle is that influence must be matched by accountability.', '陈博士：正确。算法可以帮助整理信息并识别值得关注的案件。原则是影响力必须与问责相匹配。', 170
    UNION ALL SELECT '860828100106', 134100, 143500, 'Dr. Chen: As the system''s influence grows, duties of explanation, monitoring, review, and appeal must grow with it.', '陈博士：随着系统影响力增大，解释、监测、复核和申诉义务也必须相应加强。', 180
) s ON s.item_slug = i.slug
WHERE NOT EXISTS (
    SELECT 1 FROM english_listening_segment x
    WHERE x.listening_item_id = i.id AND x.sort_order = s.sort_order
);

-- --------------------------------------------------------------------------
-- 2. 六类语音规则草稿
-- --------------------------------------------------------------------------
SET @rule_base_order := (SELECT COALESCE(MAX(sort_order), 0) FROM english_listening_pronunciation_rule);

INSERT INTO english_listening_pronunciation_rule
    (rule_type, title, slug, summary, body_markdown, audio_media_id,
     publish_status, sort_order, published_at)
SELECT s.rule_type, s.title, s.slug, s.summary, s.body_markdown, NULL,
       'DRAFT', @rule_base_order + s.seq * 10, NULL
FROM (
    SELECT 1 seq, 'LINKING' rule_type, '辅音接元音：让词与词自然连起来' title, '860828200101' slug,
           '学习最常见的辅音—元音连读，在保持清晰度的同时减少逐词停顿。' summary,
           '## 学习目标

识别并朗读辅音结尾词与元音开头词之间的自然连接，同时保留词尾辅音。

## 核心规则

当前一个词以辅音音素结尾、后一个词以元音音素开头时，英语通常取消词间停顿。判断依据是声音，不是拼写。

- `pick it up`：保留 /k/，直接连接后面的元音。
- `turn off`：/n/ 与 /ɒ/ 连续发出。
- `leave early`：/v/ 与后面的元音自然连接。

连读不是额外加音，也不是吞掉词尾音。

## 三步练习

1. 慢读两个词，确认边界音。
2. 保持辅音完整，逐渐缩短停顿。
3. 放回句子，维持自然重音。

跟读：`Please pick it up after lunch.`、`We can turn off the light.`、`I usually leave early on Friday.`' body_markdown
    UNION ALL
    SELECT 2, 'WEAK_FORM', '弱读功能词：听懂自然英语的节奏', '860828200102',
           '掌握 and、to、for、can 等功能词的弱读，理解信息词突出、功能词变轻的节奏。',
           '## 信息词与功能词

名词、主要动词、形容词和副词通常承载新信息；冠词、介词、连词和助动词主要连接语法关系，因此常被弱读。

- `and`：/ænd/ 常变为 /ən/ 或 /n/
- `to`：/tuː/ 常变为 /tə/
- `for`：/fɔːr/ 常变为 /fər/
- `can`：/kæn/ 常变为 /kən/

## 不能弱读的情况

功能词被强调、对比或单独回答时使用强读：`Yes, I CAN.`

## 对比练习

先自然读 `I can send the file to Anna.`，再对比读 `Yes, I CAN send it.`。回听时确认弱读词仍然存在，而不是完全消失。'
    UNION ALL
    SELECT 3, 'ASSIMILATION', '发音同化：相邻辅音的省力变化', '860828200103',
           '认识常见发音部位同化，理解相邻辅音为何相互影响，同时保持表达清楚。',
           '## 什么是同化

两个辅音相邻时，前一个音可能向后一个音的发音位置靠近，以减少口腔移动。拼写和词义不改变。

- `ten boys`：/n/ 在 /b/ 前可能接近 /m/
- `green park`：/n/ 在 /p/ 前可能接近 /m/
- `good boy`：/d/ 在 /b/ 前可能带有双唇倾向

## 观察方法

找到相邻辅音，观察后一个音的发音位置，再听前一个音是否向它靠近。先保证慢速形式准确，再练自然速度。

同化不是每次都必须发生，也不应为了模仿而破坏单词辨识度。'
    UNION ALL
    SELECT 4, 'ELISION', '自然省音：在辅音群中保持清晰', '860828200104',
           '学习 /t/、/d/ 在复杂辅音群中的常见弱化或省略，并以清晰度作为标准。',
           '## 核心现象

当 /t/ 或 /d/ 夹在其他辅音之间时，自然语流可能弱化或省去该音。

- `next day` 中 /t/ 可能很弱
- `old man` 中 /d/ 可能不完全释放
- `just one` 中 /t/ 可能弱化

## 安全练习原则

1. 先把每个词读准确。
2. 连接词组时缩短辅音间的过渡。
3. 不主动删除承载语法或意义的关键音。
4. 以听者能否理解作为最终标准。

每句录两遍：第一遍清晰慢读，第二遍自然朗读，再比较差异。'
    UNION ALL
    SELECT 5, 'STRESS', '句子重音：把听者注意力放在重点上', '860828200105',
           '通过移动焦点重音表达新信息、纠正和对比，建立更清楚的英语节奏。',
           '## 默认重音

名词、主要动词、形容词和副词较容易重读。重音通常同时包含音高变化、时长增加和更完整的元音。

## 对比焦点

- `MAYA sent the report.`：强调是谁。
- `Maya SENT the report.`：强调动作确实发生。
- `Maya sent the REPORT.`：强调发送的内容。
- `Maya sent it on TUESDAY.`：强调时间。

## 问答练习

用 `We need the final version today.` 分别回答“谁需要”“需要什么”“什么时候需要”，每次只突出一个信息点。'
    UNION ALL
    SELECT 6, 'INTONATION', '语调与态度：让句子表达真正意图', '860828200106',
           '认识降调、升调和先降后升的常见功能，区分确定、确认、礼貌和保留。',
           '## 三种常见走势

### 降调：完成与确定

`The meeting starts at nine. ↘`

### 升调：确认与未完成

`The meeting starts at nine? ↗`

### 先降后升：保留、对比或礼貌

`Nine is possible, but a little early. ↘↗`

## 情境练习

把 `You finished the draft` 分别读成确定陈述和确认问题，再用 `The draft is clear, but a little long` 表达温和保留。语调服务于意图，不是固定公式。'
) s
WHERE NOT EXISTS (SELECT 1 FROM english_listening_pronunciation_rule x WHERE x.slug = s.slug);

-- --------------------------------------------------------------------------
-- 3. A1-C2 写作资源草稿
-- --------------------------------------------------------------------------
SET @resource_base_order := (SELECT COALESCE(MAX(sort_order), 0) FROM english_writing_resource);

INSERT INTO english_writing_resource
    (resource_kind, expression_level, title, slug, summary, body_markdown,
     cover_media_id, cefr_level, word_min, word_max, estimated_minutes,
     template_schema_json, publish_status, sort_order, published_at)
SELECT s.resource_kind, s.expression_level, s.title, s.slug, s.summary, s.body_markdown,
       NULL, s.cefr_level, s.word_min, s.word_max, s.estimated_minutes,
       s.template_schema_json, 'DRAFT', @resource_base_order + s.seq * 10, NULL
FROM (
    SELECT 1 seq, 'EXPRESSION_LESSON' resource_kind, 'SENTENCE' expression_level,
           '用简单句写清一天的安排' title, '860828300101' slug,
           '从时间、地点和活动三个要素出发，写出清楚、可核对的 A1 日程句子。' summary,
           '## 三个必要信息

日程句首先回答：什么时候、在哪里、做什么。

- `I go to the library on Saturday.`
- `I arrive at ten o''clock.`
- `I read a book and join a reading club.`

## 常用结构

- `I go to ... at ...`
- `First, I ... Then, I ...`
- `I leave at ...`

## 完整示例

On Saturday, I go to the community library. I arrive at ten o''clock. First, I return two books. Then, I read and study English. I join the reading club at eleven. I leave the library at twelve thirty.

## 自检

检查每句是否有主语和动词，时间介词是否正确，活动是否按真实顺序出现。' body_markdown,
           'A1' cefr_level, 30 word_min, 60 word_max, 15 estimated_minutes, NULL template_schema_json
    UNION ALL
    SELECT 2, 'TEMPLATE', NULL, '周末火车旅行咨询邮件模板', '860828300102',
           '提供主题、问候、出行信息、三个核心问题和礼貌结尾，帮助完成 A2 实用邮件。',
           '## 使用方法

按区块填写真实或假设的出发地、目的地、日期和时间，不填写身份证件或支付信息。

## 结构

1. 主题：说明旅行日期与咨询目的。
2. 问候：使用 `Dear Customer Service Team,`。
3. 背景：交代路线与日期。
4. 问题：询问车次、票价和订座。
5. 结尾：感谢并署名。

## 可复用表达

- `I would like to travel from ... to ... on ...`
- `Could you please tell me which train would be suitable?`
- `I would also like to know the return fare.`
- `Do I need to reserve a seat in advance?`',
           'A2', 60, 120, 20,
           JSON_OBJECT('version', 1, 'blocks', JSON_ARRAY(
             JSON_OBJECT('id','subject','type','text','label','Subject','placeholder','Weekend train enquiry','required',TRUE),
             JSON_OBJECT('id','greeting','type','text','label','Greeting','placeholder','Dear Customer Service Team,','required',TRUE),
             JSON_OBJECT('id','travel-details','type','textarea','label','Travel details','placeholder','State the route, dates and preferred times.','required',TRUE),
             JSON_OBJECT('id','questions','type','textarea','label','Questions','placeholder','Ask about trains, fare and seat reservation.','required',TRUE),
             JSON_OBJECT('id','closing','type','textarea','label','Closing','placeholder','Thank the reader and sign your name.','required',TRUE)
           ))
    UNION ALL
    SELECT 3, 'MODEL_ESSAY', NULL, '从自动刷手机到有意识使用：一周反思', '860828300103',
           '以真实观察、微小改变和一周复盘为主线，示范如何写可信的数字习惯反思。',
           '## From Automatic Checking to Intentional Use

I used to check my phone whenever my schoolwork became difficult. The action felt like a short break, but it often interrupted my attention for much longer than I expected.

Last week, I tried one small change. During each twenty-five-minute study period, I turned off social notifications and kept the phone inside my bag. When I wanted to check it, I wrote down the reason instead. I noticed that uncertainty, rather than boredom, was often the real trigger.

The plan did not remove every interruption. However, it made the choice visible. I still used the phone for class messages during planned breaks, so the change was practical rather than extreme.

Next week, I will keep the same study periods and prepare the first step of each task before I begin. This should reduce the moments when I feel stuck. I will review the notes again instead of claiming success after only one week.

## 范文分析

文章区分了原有问题、具体措施、观察结果和下一步，没有虚构效率数据，也没有把一次尝试写成永久改变。',
           'B1', 120, 200, 30, NULL
    UNION ALL
    SELECT 4, 'GENRE_LESSON', NULL, '如何写一份可执行的远程协作建议', '860828300104',
           '学习用可观察事实定义问题，并提出包含场景、负责人、成本和复盘方式的团队建议。',
           '## 建议文的任务

建议文不是表达不满，而是让团队能够对一个明确方案作出决定。

## 推荐结构

### 1. 描述问题，不指责个人

不要写 `People do not care about documentation.`，改写为：`Important decisions are sometimes made in private messages, so other members cannot find the reasons later.`

### 2. 提出原则

说明什么信息需要公开、什么情况需要同步沟通。

### 3. 给出措施

每项措施写明适用场景、执行人和完成条件。

### 4. 说明取舍

新规则可能增加记录成本，应说明为什么成本值得承担以及如何控制。

### 5. 提出检验方式

约定试行周期，并检查决定是否可找到、行动项是否有负责人、会议是否减少重复信息。',
           'B2', 200, 300, 35, NULL
    UNION ALL
    SELECT 5, 'EXPRESSION_LESSON', 'COHESION', '平衡论证中的让步、限定与因果', '860828300105',
           '通过四类衔接关系建立有条件的 C1 论证，避免连接词堆砌和绝对化结论。',
           '## 平衡不等于各写一半

平衡论证应承认合理收益，同时界定主张成立的条件和边界。

## 四类连接关系

### 让步

`Although X may improve access, it does not guarantee accuracy.`

### 限定

`This claim applies primarily when ...`、`Only if ... can ...`

### 因果

解释中间机制：`Because the reviewer lacks relevant context, the recommendation may be accepted without meaningful scrutiny.`

### 回指

使用 `This distinction`、`Such a policy` 时，确保前文只有一个清楚对象。

## 段落骨架

提出有限定的主张，解释机制，给出情境，承认限制，最后调整主张边界。连接词不能替代真实逻辑。',
           'C1', 300, 500, 45, NULL
    UNION ALL
    SELECT 6, 'MODEL_ESSAY', NULL, 'When an Algorithm Shapes a Public Choice, Who Is Responsible?', '860828300106',
           '以公共资助排序为情境，区分设计、采购、使用与监督责任，论证责任不能被算法建议取代。',
           '## When an Algorithm Shapes a Public Choice, Who Is Responsible?

Public institutions increasingly use automated systems to rank applications, flag risks, and allocate attention. These systems may process large amounts of information consistently. Yet efficiency does not answer a prior question: when an algorithm materially shapes a public choice, who remains answerable for the result?

Developers make consequential choices about training data, target variables, thresholds, and testing. They should document foreseeable limitations and correct defects within their control. Nevertheless, development is only one stage. A technically sound model can still be unsuitable for a particular legal or social setting.

The purchasing institution has a different responsibility. Procurement teams must ask not only whether a product functions, but whether its decision role is legitimate. A contract cannot transfer a public body''s duty to explain why one applicant was favored over another.

Front-line users also exercise judgment. Treating a recommendation as compulsory may appear neutral, but it is itself an institutional choice. Users need enough information, time, and authority to question a result. It would be unfair, however, to place the entire burden on employees if management rewards automatic approval.

Responsibility should therefore be distributed without becoming diluted. Developers are accountable for technical claims; institutions for lawful purpose, procurement, and appeal; managers for operating procedures; and authorized officials for final decisions. A named owner, documented reasons, independent review, and an accessible appeal connect abstract accountability to actions that can be examined.

Some argue that human review is sufficient. A nominal reviewer who lacks time, authority, or relevant information may simply approve the machine''s output. The important distinction is not between a human and an automated decision, but between a decision that can be questioned and one protected by procedural opacity.

Algorithms do not erase responsibility. They rearrange the points at which it must be exercised. The goal is not to blame one actor for everything, but to ensure that every consequential choice has an identifiable owner and every affected person has a credible means of challenge.',
           'C2', 500, 700, 45, NULL
) s
WHERE NOT EXISTS (SELECT 1 FROM english_writing_resource x WHERE x.slug = s.slug);

-- 写作资源标签。
INSERT IGNORE INTO english_writing_resource_tag (resource_id, term_id)
SELECT r.id, t.id
FROM english_writing_resource r
JOIN (
    SELECT '860828300101' content_slug, 'topic-society' term_slug UNION ALL SELECT '860828300101','genre-application' UNION ALL SELECT '860828300101','ability-sentence' UNION ALL SELECT '860828300101','function-describe' UNION ALL
    SELECT '860828300102','topic-culture' UNION ALL SELECT '860828300102','genre-application' UNION ALL SELECT '860828300102','ability-extract' UNION ALL SELECT '860828300102','function-explain' UNION ALL
    SELECT '860828300103','topic-health' UNION ALL SELECT '860828300103','topic-tech' UNION ALL SELECT '860828300103','genre-expository' UNION ALL SELECT '860828300103','ability-cohesion' UNION ALL SELECT '860828300103','function-cause' UNION ALL
    SELECT '860828300104','topic-society' UNION ALL SELECT '860828300104','topic-economy' UNION ALL SELECT '860828300104','genre-expository' UNION ALL SELECT '860828300104','ability-organization' UNION ALL SELECT '860828300104','function-compare' UNION ALL SELECT '860828300104','function-cause' UNION ALL
    SELECT '860828300105','topic-tech' UNION ALL SELECT '860828300105','topic-society' UNION ALL SELECT '860828300105','genre-argumentative' UNION ALL SELECT '860828300105','ability-logic' UNION ALL SELECT '860828300105','ability-cohesion' UNION ALL SELECT '860828300105','function-argue' UNION ALL SELECT '860828300105','function-rebut' UNION ALL
    SELECT '860828300106','topic-politics-policy' UNION ALL SELECT '860828300106','topic-philosophy-value' UNION ALL SELECT '860828300106','topic-tech' UNION ALL SELECT '860828300106','genre-academic' UNION ALL SELECT '860828300106','ability-inference' UNION ALL SELECT '860828300106','ability-style' UNION ALL SELECT '860828300106','function-argue' UNION ALL SELECT '860828300106','function-rebut' UNION ALL SELECT '860828300106','function-summarize'
) m ON m.content_slug = r.slug
JOIN english_taxonomy_term t ON t.slug = m.term_slug AND t.enabled = 1;

-- --------------------------------------------------------------------------
-- 4. A1-C2 写作任务草稿
-- A2、B1、C2 预先关联对应草稿资源；发布前先发布资源，再发布任务。
-- --------------------------------------------------------------------------
SET @prompt_base_order := (SELECT COALESCE(MAX(sort_order), 0) FROM english_writing_prompt);

INSERT INTO english_writing_prompt
    (title, slug, summary, background_markdown, requirements_markdown,
     cefr_level, word_min, word_max, estimated_minutes, rubric_json, checklist_json,
     template_resource_id, model_resource_id, cover_media_id,
     publish_status, sort_order, published_at)
SELECT s.title, s.slug, s.summary, s.background_markdown, s.requirements_markdown,
       s.cefr_level, s.word_min, s.word_max, s.estimated_minutes,
       s.rubric_json, s.checklist_json, s.template_resource_id, s.model_resource_id,
       NULL, 'DRAFT', @prompt_base_order + s.seq * 10, NULL
FROM (
    SELECT 1 seq, '写下我的图书馆日程' title, '860828400101' slug,
           '用 30–60 个英语单词说明去图书馆的时间、活动和离开时间。' summary,
           '你计划在本周的一天去社区图书馆。朋友想知道什么时候能在那里见到你。请写一段简短日程，让对方知道你的安排。' background_markdown,
           '请用英语完成：

1. 写明星期或日期。
2. 写明到达时间和地点。
3. 写出至少两项活动。
4. 写明预计离开的时间。
5. 使用完整句子，并至少使用一次 `and`。' requirements_markdown,
           'A1' cefr_level, 30 word_min, 60 word_max, 15 estimated_minutes,
           JSON_ARRAY(JSON_OBJECT('name','任务信息完整','maxScore',40),JSON_OBJECT('name','句子结构正确','maxScore',30),JSON_OBJECT('name','时间与地点表达清楚','maxScore',20),JSON_OBJECT('name','拼写与标点','maxScore',10)) rubric_json,
           JSON_ARRAY('我写明了到达和离开的时间','我写出了至少两项活动','每个句子都有主语和动词','全文在30到60个英语单词之间') checklist_json,
           NULL template_resource_id, NULL model_resource_id
    UNION ALL
    SELECT 2, '写一封周末列车咨询邮件', '860828400102',
           '根据真实出行需求，用礼貌邮件询问列车时间、返程票价和订座要求。',
           '你计划下个月周六乘火车去邻近城市，周日傍晚返回。网站信息不足，因此需要向客服写邮件。',
           '请用英语写一封咨询邮件：

1. 写清出发地、目的地和日期。
2. 说明希望出发和返回的大致时间。
3. 询问车次、往返票价及是否需要订座。
4. 使用礼貌问句和合适的开头、结尾。
5. 不填写证件号或支付信息。',
           'A2', 60, 120, 20,
           JSON_ARRAY(JSON_OBJECT('name','出行信息完整','maxScore',30),JSON_OBJECT('name','问题明确且可回答','maxScore',30),JSON_OBJECT('name','邮件结构与礼貌程度','maxScore',25),JSON_OBJECT('name','语言准确性','maxScore',15)),
           JSON_ARRAY('主题、问候、正文和结尾齐全','我询问了车次、票价和订座','我使用了礼貌问句','全文在60到120个英语单词之间'),
           (SELECT id FROM english_writing_resource WHERE slug='860828300102'), NULL
    UNION ALL
    SELECT 3, '记录一次数字习惯改变', '860828400103',
           '描述一个影响专注或休息的数字习惯，实施一周的小改变，并以可观察事实反思。',
           '选择一个确实想改善的数字习惯。先尝试一项小改变，再回顾真实体验；若尚未实践，就写成实验计划，不虚构结果。',
           '文章应包括：

1. 原有习惯及具体影响。
2. 一到三项改变。
3. 困难与观察结果；未实践时写预期观察方法。
4. 下一步保留或调整什么。
5. 使用至少三个顺序或因果连接表达。',
           'B1', 120, 200, 30,
           JSON_ARRAY(JSON_OBJECT('name','经历或计划具体可信','maxScore',30),JSON_OBJECT('name','段落结构清楚','maxScore',25),JSON_OBJECT('name','因果与顺序衔接','maxScore',25),JSON_OBJECT('name','词汇语法准确性','maxScore',20)),
           JSON_ARRAY('我区分了事实、感受和计划','我没有编造精确的健康或效率数据','每一段都有明确作用','全文在120到200个英语单词之间'),
           NULL, (SELECT id FROM english_writing_resource WHERE slug='860828300103')
    UNION ALL
    SELECT 4, '为小型远程团队提出流程改进', '860828400104',
           '针对信息分散或责任不清的问题，写一份包含措施、取舍和复盘方法的团队建议。',
           '一个六人远程团队同时使用群聊、邮件和项目管理工具。重要决定散落在私人消息里，行动项经常没有负责人。',
           '请写一份英语建议：

1. 用可观察事实定义两个问题。
2. 提出两到三项具体规则及适用场景。
3. 比较同步与异步沟通的优缺点。
4. 说明新规则的成本或阻力。
5. 给出两周后复盘时要检查的证据。',
           'B2', 200, 300, 35,
           JSON_ARRAY(JSON_OBJECT('name','问题定义准确','maxScore',20),JSON_OBJECT('name','建议具体可执行','maxScore',30),JSON_OBJECT('name','比较与取舍充分','maxScore',25),JSON_OBJECT('name','结构和语言清晰','maxScore',25)),
           JSON_ARRAY('我描述了行为而不是指责个人','每项措施都有场景或条件','我说明了至少一个成本或限制','全文在200到300个英语单词之间'),
           NULL, NULL
    UNION ALL
    SELECT 5, 'AI 可以辅助什么，又不应决定什么', '860828400105',
           '围绕教育中的反馈、评估与责任，写一篇有条件、有反方回应的平衡论证。',
           '一所学校计划让生成式 AI 同时参与日常反馈和期末评分。教师看到即时反馈的价值，学生担心错误、隐私和申诉渠道。',
           '请完成一篇英语论证文章：

1. 区分低风险辅助与高影响决定。
2. 分析至少两项收益和两项限制。
3. 公平回应一个支持扩大使用的理由。
4. 提出教师责任、信息披露和申诉条件。
5. 使用让步、限定、因果和回指衔接。',
           'C1', 300, 500, 45,
           JSON_ARRAY(JSON_OBJECT('name','中心立场与边界','maxScore',25),JSON_OBJECT('name','论证深度与反方回应','maxScore',30),JSON_OBJECT('name','衔接与段落推进','maxScore',25),JSON_OBJECT('name','语言准确与正式程度','maxScore',20)),
           JSON_ARRAY('结论说明了适用条件','每个因果判断解释了中间机制','代词和回指有清楚对象','全文在300到500个英语单词之间'),
           NULL, NULL
    UNION ALL
    SELECT 6, '为算法辅助的公共决策设计问责框架', '860828400106',
           '分析开发者、采购机构、管理者与决策人员的责任，并提出可审查、可申诉的治理框架。',
           '某市准备用算法为社区项目资助申请排序。系统由外部公司开发，市政府采购，工作人员据此提出建议，公共委员会作最终决定。',
           '请写一篇正式英语文章：

1. 解释为何人类签字不必然等于有效监督。
2. 界定开发者、采购机构、管理者、使用者和决策者的责任。
3. 讨论透明度与商业保密的冲突。
4. 设计至少三项可操作保障。
5. 回应“分布式责任导致无人负责”的意见。
6. 不虚构城市、产品、准确率或人数。',
           'C2', 500, 700, 60,
           JSON_ARRAY(JSON_OBJECT('name','概念区分与问题建模','maxScore',25),JSON_OBJECT('name','多方责任论证','maxScore',25),JSON_OBJECT('name','反方回应与价值权衡','maxScore',20),JSON_OBJECT('name','治理措施可执行性','maxScore',20),JSON_OBJECT('name','学术风格与语言控制','maxScore',10)),
           JSON_ARRAY('我没有把技术错误与制度责任混为一谈','每项保障对应一个明确风险','我回应了最有力的反对意见','我没有虚构统计或产品能力','全文在500到700个英语单词之间'),
           NULL, (SELECT id FROM english_writing_resource WHERE slug='860828300106')
) s
WHERE NOT EXISTS (SELECT 1 FROM english_writing_prompt x WHERE x.slug = s.slug);

-- 写作任务标签。
INSERT IGNORE INTO english_writing_prompt_tag (prompt_id, term_id)
SELECT p.id, t.id
FROM english_writing_prompt p
JOIN (
    SELECT '860828400101' content_slug, 'topic-society' term_slug UNION ALL SELECT '860828400101','genre-application' UNION ALL SELECT '860828400101','ability-sentence' UNION ALL SELECT '860828400101','function-describe' UNION ALL
    SELECT '860828400102','topic-culture' UNION ALL SELECT '860828400102','genre-application' UNION ALL SELECT '860828400102','ability-extract' UNION ALL SELECT '860828400102','function-explain' UNION ALL
    SELECT '860828400103','topic-health' UNION ALL SELECT '860828400103','topic-tech' UNION ALL SELECT '860828400103','genre-expository' UNION ALL SELECT '860828400103','ability-cohesion' UNION ALL SELECT '860828400103','function-cause' UNION ALL
    SELECT '860828400104','topic-society' UNION ALL SELECT '860828400104','topic-economy' UNION ALL SELECT '860828400104','genre-expository' UNION ALL SELECT '860828400104','ability-organization' UNION ALL SELECT '860828400104','function-compare' UNION ALL SELECT '860828400104','function-cause' UNION ALL
    SELECT '860828400105','topic-tech' UNION ALL SELECT '860828400105','topic-society' UNION ALL SELECT '860828400105','genre-argumentative' UNION ALL SELECT '860828400105','ability-logic' UNION ALL SELECT '860828400105','function-argue' UNION ALL SELECT '860828400105','function-rebut' UNION ALL
    SELECT '860828400106','topic-politics-policy' UNION ALL SELECT '860828400106','topic-philosophy-value' UNION ALL SELECT '860828400106','topic-tech' UNION ALL SELECT '860828400106','genre-academic' UNION ALL SELECT '860828400106','ability-inference' UNION ALL SELECT '860828400106','function-argue' UNION ALL SELECT '860828400106','function-rebut'
) m ON m.content_slug = p.slug
JOIN english_taxonomy_term t ON t.slug = m.term_slug AND t.enabled = 1;

-- --------------------------------------------------------------------------
-- 5. 六个词族与 27 个成员
-- 当前数据库结构没有词族发布状态；这些记录写入后会立即对词族接口可见。
-- --------------------------------------------------------------------------
INSERT INTO vocabulary_word_family (head_word, slug, description)
SELECT s.head_word, s.slug, s.description
FROM (
    SELECT 'use' head_word, '860828500101' slug, '围绕使用、用途与价值判断的常用派生词，并提示 use 作名词与动词时的读音差异。' description UNION ALL
    SELECT 'travel', '860828500102', '围绕旅行者、旅行活动和旅行经历组织的词族，适合 A2 出行情境。' UNION ALL
    SELECT 'decide', '860828500103', '从作出决定扩展到决定、果断程度和犹豫状态，用于个人与公共决策。' UNION ALL
    SELECT 'create', '860828500104', '围绕创造行为、成果、创造者与创造力展开，适用于学习、设计和协作。' UNION ALL
    SELECT 'analyze', '860828500105', '围绕分析动作、结果、人员与方法，服务于 B2-C2 的论证和研究表达。' UNION ALL
    SELECT 'govern', '860828500106', '从管理和支配扩展到政府、治理机制与制度属性，用于公共决策和问责主题。'
) s
WHERE NOT EXISTS (SELECT 1 FROM vocabulary_word_family x WHERE x.slug = s.slug);

INSERT INTO vocabulary_family_member
    (family_id, spelling, part_of_speech, phonetic_us, translation, cefr_level,
     example_sentence, example_translation, sort_order)
SELECT f.id, m.spelling, m.part_of_speech, m.phonetic_us, m.translation, m.cefr_level,
       m.example_sentence, m.example_translation, m.sort_order
FROM vocabulary_word_family f
JOIN (
    SELECT '860828500101' family_slug, 'useful' spelling, 'adjective' part_of_speech, '/ˈjuːsfəl/' phonetic_us, '有用的；实用的' translation, 'A2' cefr_level, 'This checklist is useful when you review your writing.' example_sentence, '这份清单在你检查写作时很有用。' example_translation, 10 sort_order UNION ALL
    SELECT '860828500101','user','noun','/ˈjuːzər/','使用者；用户','A2','Each user can choose a different learning path.','每位用户都可以选择不同的学习路径。',20 UNION ALL
    SELECT '860828500101','useless','adjective','/ˈjuːsləs/','无用的；不起作用的','B1','The data is useless unless we know how it was collected.','如果不知道数据如何收集，它就没有用。',30 UNION ALL
    SELECT '860828500101','usage','noun','/ˈjuːsɪdʒ/','使用；用法；使用量','B2','The team reviewed its energy usage at the end of the month.','团队在月底检查了能源使用情况。',40 UNION ALL
    SELECT '860828500101','reusable','adjective','/ˌriːˈjuːzəbəl/','可重复使用的','B2','We replaced disposable cups with reusable bottles.','我们用可重复使用的水瓶替代了一次性杯子。',50 UNION ALL

    SELECT '860828500102','traveler','noun','/ˈtrævələr/','旅行者','A2','Every traveler should check the return time before leaving.','每位旅行者出发前都应核对返程时间。',10 UNION ALL
    SELECT '860828500102','traveling','noun/adjective','/ˈtrævəlɪŋ/','旅行；旅行中的','A2','Traveling by train gives us time to read and rest.','乘火车旅行让我们有时间阅读和休息。',20 UNION ALL
    SELECT '860828500102','traveled','adjective','/ˈtrævəld/','旅行经历丰富的；走过的','B1','This is a well-traveled route between the two cities.','这是两座城市之间一条繁忙而常走的路线。',30 UNION ALL
    SELECT '860828500102','travel-related','adjective','/ˈtrævəl rɪˌleɪtɪd/','与旅行有关的','B2','Keep all travel-related documents in one secure folder.','将所有旅行相关文件保存在一个安全文件夹中。',40 UNION ALL

    SELECT '860828500103','decision','noun','/dɪˈsɪʒən/','决定；决策','B1','The committee recorded the reason for its final decision.','委员会记录了最终决定的理由。',10 UNION ALL
    SELECT '860828500103','decisive','adjective','/dɪˈsaɪsɪv/','决定性的；果断的','C1','Clear evidence played a decisive role in the review.','清楚的证据在审查中起了决定性作用。',20 UNION ALL
    SELECT '860828500103','decisively','adverb','/dɪˈsaɪsɪvli/','果断地；决定性地','C1','The manager acted decisively after the risk was confirmed.','风险确认后，经理果断采取了行动。',30 UNION ALL
    SELECT '860828500103','indecisive','adjective','/ˌɪndɪˈsaɪsɪv/','犹豫不决的；不明确的','C1','The report offered options but no criteria, so its conclusion was indecisive.','报告只给出选项却没有标准，因此结论不明确。',40 UNION ALL

    SELECT '860828500104','creation','noun','/kriˈeɪʃən/','创造；创作物','B1','The creation of a shared guide reduced repeated questions.','共享指南的建立减少了重复问题。',10 UNION ALL
    SELECT '860828500104','creator','noun','/kriˈeɪtər/','创作者；创造者','B1','The creator explained why the lesson uses original examples.','创作者解释了课程为何使用原创例句。',20 UNION ALL
    SELECT '860828500104','creative','adjective','/kriˈeɪtɪv/','有创造力的；创意的','B1','The team found a creative way to test the idea cheaply.','团队找到了一种低成本检验想法的创新方法。',30 UNION ALL
    SELECT '860828500104','creativity','noun','/ˌkriːeɪˈtɪvəti/','创造力','B2','Clear limits can sometimes support creativity.','清晰的限制有时能够促进创造力。',40 UNION ALL
    SELECT '860828500104','creatively','adverb','/kriˈeɪtɪvli/','创造性地','B2','Students used the available materials creatively.','学生们创造性地利用了现有材料。',50 UNION ALL

    SELECT '860828500105','analysis','noun','/əˈnæləsɪs/','分析','B2','Our analysis separates observed facts from possible explanations.','我们的分析把观察事实与可能解释区分开来。',10 UNION ALL
    SELECT '860828500105','analyst','noun','/ˈænəlɪst/','分析人员；分析师','B2','The analyst checked whether the groups were compared fairly.','分析人员检查了两组对象是否得到公平比较。',20 UNION ALL
    SELECT '860828500105','analytical','adjective','/ˌænəˈlɪtɪkəl/','分析性的；善于分析的','C1','Analytical writing makes each step of an argument visible.','分析性写作会清楚呈现论证的每一步。',30 UNION ALL
    SELECT '860828500105','analytically','adverb','/ˌænəˈlɪtɪkli/','分析地；从分析角度','C1','We should examine the claim analytically before accepting it.','接受主张前，我们应从分析角度审视它。',40 UNION ALL

    SELECT '860828500106','government','noun','/ˈɡʌvərnmənt/','政府；治理机构','A2','The local government published the meeting agenda online.','当地政府在网上公布了会议议程。',10 UNION ALL
    SELECT '860828500106','governor','noun','/ˈɡʌvərnər/','州长；管理者；调节器','B2','The governor asked the agency to explain its decision process.','州长要求该机构解释决策过程。',20 UNION ALL
    SELECT '860828500106','governance','noun','/ˈɡʌvərnəns/','治理；治理体系','C1','Good governance requires clear responsibility and a route of appeal.','良好治理需要明确责任和申诉渠道。',30 UNION ALL
    SELECT '860828500106','governmental','adjective','/ˌɡʌvərnˈmentəl/','政府的；治理机构的','C1','The study examined several governmental review procedures.','研究考察了多种政府审查程序。',40 UNION ALL
    SELECT '860828500106','ungoverned','adjective','/ʌnˈɡʌvərnd/','未受治理或约束的','C2','An ungoverned system can distribute harm without clear accountability.','缺乏治理的系统可能在责任不明时扩散伤害。',50
) m ON m.family_slug = f.slug
WHERE NOT EXISTS (
    SELECT 1 FROM vocabulary_family_member x
    WHERE x.family_id = f.id AND x.spelling = m.spelling
);

-- 每个词族关联词库中拼写相同且 id 最小的现有主词，不新增主词。
INSERT IGNORE INTO vocabulary_word_family_link (family_id, word_id)
SELECT f.id,
       (SELECT MIN(w.id) FROM vocabulary_word w WHERE LOWER(w.word) = LOWER(f.head_word))
FROM vocabulary_word_family f
WHERE f.slug IN ('860828500101','860828500102','860828500103','860828500104','860828500105','860828500106')
  AND EXISTS (SELECT 1 FROM vocabulary_word w WHERE LOWER(w.word) = LOWER(f.head_word));

COMMIT;

-- --------------------------------------------------------------------------
-- 6. 导入结果校验（只读）
-- --------------------------------------------------------------------------
SELECT 'listening_drafts' item, COUNT(*) count
FROM english_listening_item
WHERE slug IN ('860828100101','860828100102','860828100103','860828100104','860828100105','860828100106')
  AND publish_status='DRAFT' AND published_at IS NULL
UNION ALL
SELECT 'pronunciation_drafts', COUNT(*)
FROM english_listening_pronunciation_rule
WHERE slug IN ('860828200101','860828200102','860828200103','860828200104','860828200105','860828200106')
  AND publish_status='DRAFT' AND published_at IS NULL
UNION ALL
SELECT 'writing_resource_drafts', COUNT(*)
FROM english_writing_resource
WHERE slug IN ('860828300101','860828300102','860828300103','860828300104','860828300105','860828300106')
  AND publish_status='DRAFT' AND published_at IS NULL
UNION ALL
SELECT 'writing_prompt_drafts', COUNT(*)
FROM english_writing_prompt
WHERE slug IN ('860828400101','860828400102','860828400103','860828400104','860828400105','860828400106')
  AND publish_status='DRAFT' AND published_at IS NULL
UNION ALL
SELECT 'word_families_no_status_column', COUNT(*)
FROM vocabulary_word_family
WHERE slug IN ('860828500101','860828500102','860828500103','860828500104','860828500105','860828500106')
UNION ALL
SELECT 'word_family_members', COUNT(*)
FROM vocabulary_family_member m
JOIN vocabulary_word_family f ON f.id=m.family_id
WHERE f.slug IN ('860828500101','860828500102','860828500103','860828500104','860828500105','860828500106')
UNION ALL
SELECT 'word_family_main_word_links', COUNT(*)
FROM vocabulary_word_family_link l
JOIN vocabulary_word_family f ON f.id=l.family_id
WHERE f.slug IN ('860828500101','860828500102','860828500103','860828500104','860828500105','860828500106');

SELECT 'listening_segments' item, COUNT(*) count
FROM english_listening_segment s
JOIN english_listening_item i ON i.id=s.listening_item_id
WHERE i.slug BETWEEN '860828100101' AND '860828100106'
UNION ALL
SELECT 'listening_items_without_audio_expected_6', COUNT(*)
FROM english_listening_item
WHERE slug BETWEEN '860828100101' AND '860828100106' AND audio_media_id IS NULL
UNION ALL
SELECT 'invalid_or_overflow_segments_must_be_zero', COUNT(*)
FROM english_listening_segment s
JOIN english_listening_item i ON i.id=s.listening_item_id
WHERE i.slug BETWEEN '860828100101' AND '860828100106'
  AND (s.start_ms < 0 OR s.end_ms <= s.start_ms OR s.end_ms > i.duration_seconds * 1000
       OR TRIM(s.transcript_text) = '')
UNION ALL
SELECT 'overlapping_segments_must_be_zero', COUNT(*)
FROM english_listening_segment a
JOIN english_listening_segment b
  ON b.listening_item_id=a.listening_item_id AND b.sort_order>a.sort_order
JOIN english_listening_item i ON i.id=a.listening_item_id
WHERE i.slug BETWEEN '860828100101' AND '860828100106'
  AND b.start_ms < a.end_ms;

SELECT 'non_draft_publish_rows_must_be_zero' check_name,
       (SELECT COUNT(*) FROM english_listening_item WHERE slug BETWEEN '860828100101' AND '860828100106' AND publish_status <> 'DRAFT')
     + (SELECT COUNT(*) FROM english_listening_pronunciation_rule WHERE slug BETWEEN '860828200101' AND '860828200106' AND publish_status <> 'DRAFT')
     + (SELECT COUNT(*) FROM english_writing_resource WHERE slug BETWEEN '860828300101' AND '860828300106' AND publish_status <> 'DRAFT')
     + (SELECT COUNT(*) FROM english_writing_prompt WHERE slug BETWEEN '860828400101' AND '860828400106' AND publish_status <> 'DRAFT') AS count;
