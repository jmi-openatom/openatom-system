import { cp, mkdir, readFile, readdir, writeFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { resolve, join } from 'node:path';

const destination = resolve(process.argv[2] || '.guide-release');
const commit = process.env.GUIDE_COMMIT;
const runNumber = Number(process.env.GUIDE_RUN_NUMBER);
if (!/^[a-f0-9]{40}$/.test(commit || '') || !Number.isSafeInteger(runNumber) || runNumber < 1) {
  throw new Error('GUIDE_COMMIT and a positive GUIDE_RUN_NUMBER are required.');
}
await mkdir(destination, { recursive: false });
const publicDir = join(destination, 'public');
await cp(new URL('../dist/', import.meta.url), publicDir, { recursive: true });
await writeFile(join(publicDir, 'release.json'), JSON.stringify({ commit, runNumber }) + '\n');
const files = {};
async function hashDirectory(directory, prefix = '') {
  for (const entry of (await readdir(directory, { withFileTypes: true })).sort((a,b) => a.name.localeCompare(b.name))) {
    const name = `${prefix}${entry.name}`;
    if (entry.isDirectory()) await hashDirectory(join(directory, entry.name), `${name}/`);
    else if (entry.isFile()) files[name] = createHash('sha256').update(await readFile(join(directory, entry.name))).digest('hex');
    else throw new Error(`Unsupported release entry: ${name}`);
  }
}
await hashDirectory(publicDir);
await writeFile(join(destination, 'manifest.json'), JSON.stringify({ commit, files }, null, 2) + '\n');
await cp(new URL('../deploy/publish.py', import.meta.url), join(destination, 'publish.py'));
console.log(`Packaged ${Object.keys(files).length} files at ${destination}`);
