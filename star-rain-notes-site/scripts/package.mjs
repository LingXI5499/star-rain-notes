import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const result=spawnSync('python',[path.join(root,'scripts/package.py')],{cwd:root,stdio:'inherit'});
process.exit(result.status??1);
