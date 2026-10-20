import { cp, mkdir, rm, copyFile } from 'node:fs/promises';
const root = new URL('../', import.meta.url);
const dist = new URL('dist/', root);
await rm(dist, { recursive: true, force: true });
await mkdir(dist, { recursive: true });
for (const file of ['index.html', 'app.js', 'content.js', 'walkthroughs.js', 'styles.css']) {
  await copyFile(new URL(file, root), new URL(file, dist));
}
await cp(new URL('assets/', root), new URL('assets/', dist), { recursive: true });
console.log(`Built standalone static site: ${dist.pathname}`);
