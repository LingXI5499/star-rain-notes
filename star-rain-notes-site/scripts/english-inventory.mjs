import fs from 'node:fs';
import path from 'node:path';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';

export const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
export const dataRoot = path.join(root, 'src/content-data');
export const read = p => JSON.parse(fs.readFileSync(path.join(dataRoot, p), 'utf8'));
export const digest = file => createHash('sha256').update(fs.readFileSync(file)).digest('hex');
export function inventory() {
  const themes = read('english/themes.json');
  const records = [], duplicates = new Map(), spellings = new Map();
  const fields = ['word','partOfSpeech','phoneticUs','phoneticUk','translation','sceneMeaning','examples'];
  const report = { legacyThemes: themes.length, declared: 0, actual: 0, themes: [], missingFields: {}, exampleShapes: {}, duplicateGroups: [], spellingGroups: [], sourceHashes: {} };
  for (const theme of themes) {
    const relative = `english/vocabulary/${theme.id}.json`, source = read(relative);
    report.sourceHashes[relative] = digest(path.join(dataRoot, relative));
    const item = { id: theme.id, title: theme.title, declared: theme.wordCount, detailDeclared: source.wordCount, actual: source.words.length, difference: source.words.length - theme.wordCount, missingFields: {}, empty: !source.words.length };
    for (const [index, word] of source.words.entries()) {
      const entryId = `legacy-${theme.id}-${String(index + 1).padStart(4, '0')}`;
      records.push({ ...word, entryId, source: { legacyThemeId: theme.id, legacyIndex: index } });
      for (const key of fields) if (!(key in word) || word[key] === '' || word[key] == null) { item.missingFields[key] = (item.missingFields[key] || 0) + 1; report.missingFields[key] = (report.missingFields[key] || 0) + 1; }
      const shape = typeof word.examples === 'string' ? 'JSON string' : Array.isArray(word.examples) ? 'array' : typeof word.examples;
      report.exampleShapes[shape] = (report.exampleShapes[shape] || 0) + 1;
      const signature = JSON.stringify(word);
      duplicates.set(signature, [...(duplicates.get(signature) || []), entryId]);
      const spelling = word.word.trim().toLowerCase();
      spellings.set(spelling, [...(spellings.get(spelling) || []), entryId]);
    }
    report.declared += item.declared; report.actual += item.actual; report.themes.push(item);
  }
  report.duplicateGroups = [...duplicates.values()].filter(ids => ids.length > 1);
  report.spellingGroups = [...spellings].filter(([,ids]) => ids.length > 1).map(([word, entryIds]) => ({ word, entryIds }));
  report.sourceHashes['english/themes.json'] = digest(path.join(dataRoot, 'english/themes.json'));
  report.sourceHashes['english/grammar.json'] = digest(path.join(dataRoot, 'english/grammar.json'));
  const walk = dir => fs.readdirSync(dir, { withFileTypes: true }).flatMap(e => e.isDirectory() ? walk(path.join(dir,e.name)) : [path.join(dir,e.name)]);
  const grammarBodies = walk(path.join(dataRoot, 'english/grammar')).filter(f => f.endsWith('.html'));
  for (const file of grammarBodies) report.sourceHashes[path.relative(dataRoot,file).replaceAll('\\','/')] = digest(file);
  report.grammar = { sections: read('english/grammar.json').sections.length, lessons: grammarBodies.length };
  return { report, records };
}
if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const { report } = inventory();
  fs.mkdirSync(path.join(root,'docs'), { recursive: true });
  const destination = path.join(root,'docs/english-inventory.json');
  if (fs.existsSync(destination) && fs.readFileSync(destination,'utf8') !== JSON.stringify(report,null,2)+'\n') throw new Error('Inventory changed: review the original baseline before replacing it');
  fs.writeFileSync(destination, JSON.stringify(report,null,2)+'\n');
  console.log(JSON.stringify({ themes: report.legacyThemes, declared: report.declared, actual: report.actual, emptyThemes: report.themes.filter(t=>t.empty).map(t=>t.id), missingFields: report.missingFields, exampleShapes: report.exampleShapes, exactDuplicateRecords: report.duplicateGroups.flat().length, repeatedSpellingRecords: report.spellingGroups.flatMap(g=>g.entryIds).length, grammar:report.grammar }));
}
