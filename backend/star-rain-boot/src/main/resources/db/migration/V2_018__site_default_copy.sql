-- Preserve the existing V1-inspired homepage copy for untouched local seed data.
UPDATE sr_site_config
SET site_title = '沿时间沉淀思考，让经验持续生长。',
    site_description = '星雨笔录记录技术实践、学习路径与系统复盘。',
    home_intro = '星雨笔录记录技术实践、学习路径与系统复盘。把判断写下来，把过程留下来，让下一次出发有迹可循。',
    footer_text = '在知识、代码与成长之间，留下可以回看的坐标。'
WHERE config_key = 'PRIMARY'
  AND site_title = '星雨笔录'
  AND home_intro = '在这里整理学习路径、技术文章与作品。';
