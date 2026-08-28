/**
 * make-tutorial-sql.mjs — 从 V5 迁移生成宝塔可直接执行的教程数据 SQL 文件。
 * 读取 V5__seed_tutorial_taxonomy.sql（UTF-8），提取 3 条 INSERT，写入
 * yulanlin/tutorial-taxonomy-data.sql 并加中文说明头。
 */
import { readFileSync, writeFileSync } from 'node:fs'

const v5 = readFileSync(
  'backend/src/main/resources/db/migration/V5__seed_tutorial_taxonomy.sql',
  'utf8',
)
const inserts = [...v5.matchAll(/INSERT INTO `[a-z_]+` VALUES \([^;]+\);/g)].map((m) => m[0])

const header = `-- ============================================================
-- 星雨笔录 · 教程体系 V2 数据补录
-- 内容：教程分类(5) / 教程(55) / 分组+章节(991 节点，含发布状态)
-- 目标：宝塔 star 数据库（教程表当前为空时执行）
-- 执行（SSH 终端）：
--     mysql -ustar -p1234 star < tutorial-taxonomy-data.sql
-- 或宝塔 phpMyAdmin → star 数据库 → 导入本文件。
-- 注意：按 dev 库固定 ID 插入，教程表已有数据时勿执行。
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

`

const footer = `

SET FOREIGN_KEY_CHECKS=1;
`

writeFileSync('yulanlin/tutorial-taxonomy-data.sql', header + inserts.join('\n\n') + footer, 'utf8')
console.log(`INSERT 语句数: ${inserts.length}`)
console.log(`表: ${inserts.map((i) => i.split(' ')[2]).join(', ')}`)
