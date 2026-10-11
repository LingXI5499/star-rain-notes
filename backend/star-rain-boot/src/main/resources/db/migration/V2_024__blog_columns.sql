-- 博客专栏（专题）初始化。
--
-- 「首页」不是一个专题，它是 /blog 全部文章，因此不在这里建。
-- 这里只建所有者已经确定要有的五个内容专栏；以后新增专栏走后台「博客 / 分类与专题」，
-- 不需要再写迁移。
--
-- 幂等：sr_blog_topic 在 slug 上有唯一键 uk_sr_blog_topic_slug，
-- 用 INSERT IGNORE 重复执行不会产生第二份，也不会覆盖后台改过的名字。
INSERT IGNORE INTO sr_blog_topic (slug, name, description, status) VALUES
  ('leetcode',       '力扣',     '力扣题解与算法练习记录',       'ENABLED'),
  ('data-structure', '数据结构', '线性表、树、图与常见算法的时间空间取舍', 'ENABLED'),
  ('design-pattern', '设计模式', '从真实代码里看设计模式解决了什么问题',   'ENABLED'),
  ('novel',          '小说',     '长篇与短篇写作',               'ENABLED'),
  ('worldview',      '世界观',   '设定、地理、势力与时间线',       'ENABLED');

-- 早先导入 V1 内容时建的占位专题 test 不该出现在前台专栏栏里。
-- 用停用而不是删除：专题页与文章关联都还在，后台随时可以改名启用。
UPDATE sr_blog_topic SET status = 'DISABLED' WHERE slug = 'test';
