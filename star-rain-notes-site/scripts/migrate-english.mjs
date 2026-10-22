import fs from 'node:fs';
import path from 'node:path';
import { inventory, read, root } from './english-inventory.mjs';
import { mappingRules } from './english-mapping-rules.mjs';

export function migrateRecord(record) {
  const examples = typeof record.examples === 'string' ? JSON.parse(record.examples) : record.examples;
  if (!Array.isArray(examples)) throw new Error(`${record.entryId}: examples must decode to an array`);
  const evidence = mappingRules.filter(r => new RegExp(r.pattern).test(record.sceneMeaning));
  const agreed = evidence.filter(r => new RegExp(r.pattern).test(record.translation));
  const special = Number(record.source.legacyThemeId) >= 55;
  const confirmed = !special && agreed.length > 0 && agreed.length <= 3 && evidence.length === agreed.length;
  const migrationStatus = confirmed ? 'confirmed' : evidence.length || special ? 'needs-review' : 'unclassified';
  return { ...record, examples, topicCodes: confirmed ? agreed.map(r=>r.code) : [], suggestedTopicCodes: confirmed ? [] : evidence.map(r=>r.code), migrationStatus,
    migrationReason: confirmed ? '原释义与主题语境均命中明确语义规则；保留原记录，不合并词义。' : special ? '旧语言系统主题：需逐条核实普通词、功能词、词根词缀或表达材料的属性及归属。' : evidence.length ? '语境与原释义的分类证据不一致或涉及较多主题，需复核词性与词义。' : '没有可靠的自动映射证据，保留旧主题供人工整理。',
    mappingEvidence: evidence.map(r=>({topicCode:r.code,scene:record.sceneMeaning.match(new RegExp(r.pattern))?.[0],translation:record.translation.match(new RegExp(r.pattern))?.[0]||null})),
    contentKind: special ? (/^(art|prep|conj|pron|aux)(\b|\s|\/)/.test(record.partOfSpeech) ? 'function-word' : /[\s/]/.test(record.word) ? 'expression-or-multiword' : 'lexical-record-needs-review') : 'lexical-record',
  };
}
const { report: current, records } = inventory();
const baseline = JSON.parse(fs.readFileSync(path.join(root,'docs/english-inventory.json'),'utf8'));
if (JSON.stringify(baseline.sourceHashes) !== JSON.stringify(current.sourceHashes)) throw new Error('Legacy sources changed since inventory');
const entries = records.map(migrateRecord);
const taxonomy = read('english/vocabulary-taxonomy.json');
const topics = taxonomy.categories.flatMap(c=>c.topics);
const count = status => entries.filter(e=>e.migrationStatus===status).length;
const byTopic = topics.map(t=>({code:t.code,title:t.title,count:entries.filter(e=>e.topicCodes.includes(t.code)).length}));
const byLegacy = current.themes.map(t=>({...t,confirmed:entries.filter(e=>e.source.legacyThemeId===t.id&&e.migrationStatus==='confirmed').length,topicCodes:[...new Set(entries.filter(e=>e.source.legacyThemeId===t.id).flatMap(e=>e.topicCodes))]}));
const review = entries.filter(e=>e.migrationStatus!=='confirmed').map(e=>({entryId:e.entryId,source:e.source,word:e.word,partOfSpeech:e.partOfSpeech,translation:e.translation,sceneMeaning:e.sceneMeaning,status:e.migrationStatus,contentKind:e.contentKind,suggestedTopicCodes:e.suggestedTopicCodes,reason:e.migrationReason}));
const report={sourceRecords:records.length,migratedRecords:entries.length,confirmed:count('confirmed'),needsReview:count('needs-review'),unclassified:count('unclassified'),suspectedExactDuplicateRecords:current.duplicateGroups.flat().length,repeatedSpellingRecords:current.spellingGroups.flatMap(g=>g.entryIds).length,removedRecords:0,mergedRecords:0,normalizedExampleRecords:entries.length,taxonomyDifference:{expectedFromImage:168,enumerated:topics.length,status:'needs-confirmation'},byLegacy,byTopic,emptyTopics:byTopic.filter(t=>!t.count).map(t=>t.code)};
const outputs={
  'src/content-data/english/vocabulary-entries.json':{schemaVersion:1,entries},
  'docs/english-migration.json':report,
  'docs/english-review-queue.json':review,
};
for(const [relative,value] of Object.entries(outputs)){
  const target=path.join(root,relative), content=JSON.stringify(value,null,2)+'\n';
  if(process.argv.includes('--check')){if(!fs.existsSync(target)||fs.readFileSync(target,'utf8')!==content)throw new Error(`${relative}: stale migration output`);}
  else fs.writeFileSync(target,content);
}
const md=`# 英语词汇数据盘点与迁移报告\n\n来源：60 个保留的旧主题 JSON；逐条来源使用旧主题 ID 和原数组下标。旧源文件和语法正文 SHA-256 见 english-inventory.json。\n\n- 声明词条：${current.declared}；实际记录：${records.length}；迁移记录：${entries.length}。\n- 已确认：${report.confirmed}；待复核：${report.needsReview}；未分类：${report.unclassified}。\n- 完全相同字段的疑似重复记录：${report.suspectedExactDuplicateRecords}；重复拼写涉及记录：${report.repeatedSpellingRecords}。两者口径不同，均未合并或删除。\n- 例句字符串转数组：${entries.length}；原文件保留原始字符串。\n- 缺失音标：美式 ${current.missingFields.phoneticUs||0}，英式 ${current.missingFields.phoneticUk||0}；没有补造。\n- 参考图 168 项，可辨识 167 项；status = needs-confirmation；缺失分类仍待确认。\n\n## 映射方法与局限\n\n只在原释义和语境释义均命中同一明确语义规则、且无其他冲突线索时确认映射；多义记录允许多主题，按记录 ID 独立保留。自动规则不等于逐条人工语言学审定。语义不一致、涉及过多主题及旧主题 55–60 的材料进入复核队列，不强塞分类。复核建议不进入新分类计数；旧页仍展示全部记录。规则位于 scripts/english-mapping-rules.mjs，逐条证据保存在 vocabulary-entries.json。\n\n## 旧主题与新主题对照\n\n|旧 ID|旧主题|声明|实际|已确认|新主题|\n|---|---|---:|---:|---:|---|\n${byLegacy.map(t=>`|${t.id}|${t.title}|${t.declared}|${t.actual}|${t.confirmed}|${t.topicCodes.join('、')||'待整理'}|`).join('\n')}\n\n## 新主题真实词条数\n\n主题计数可重复计入多主题记录，不代表全站唯一词条数。全站按 entryId 共有 ${entries.length} 条来源记录。\n\n|代码|主题|已确认记录|\n|---|---|---:|\n${byTopic.map(t=>`|${t.code}|${t.title}|${t.count}|`).join('\n')}\n\n## 待决与特殊内容\n\n- 空旧主题：${current.themes.filter(t=>t.empty).map(t=>t.id).join('、')}，保留兼容页。\n- 空新主题：${report.emptyTopics.join('、')||'无'}。\n- 全部待复核/未分类条目的来源、词性、原释义、语境、建议分类、原因见 english-review-queue.json；不是抽样清单。\n- 完全重复组和重复拼写组逐条 ID 见 english-inventory.json，当前没有执行合并。\n- 55–60 主题的全部非空记录均保留并进入复核清单，contentKind 区分功能词、多词表达及需复核的普通词记录。主题 58 实际为零。\n- 需要确认参考图缺失的第 168 个分类；需要人工审阅待复核词义及特殊语言材料归属。\n`;
const mdPath=path.join(root,'docs/english-taxonomy-migration-report.md');
if(!process.argv.includes('--check'))fs.writeFileSync(mdPath,md);
else if(fs.readFileSync(mdPath,'utf8')!==md)throw new Error('Stale migration markdown');
console.log(`English migration ${process.argv.includes('--check')?'verified':'written'}: ${entries.length} records; ${report.confirmed} confirmed, ${report.needsReview} review, ${report.unclassified} unclassified; zero removed/merged.`);
