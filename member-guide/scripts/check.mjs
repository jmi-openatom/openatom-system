import assert from 'node:assert/strict';
import { readFile, access } from 'node:fs/promises';
import { execFileSync } from 'node:child_process';
import { articles, checklist, homeSections } from '../content.js';
import { allSteps } from '../walkthroughs.js';

const root = new URL('../', import.meta.url);
for (const path of ['app.js', 'content.js', 'walkthroughs.js', 'scripts/serve.mjs', 'scripts/build.mjs', 'scripts/figures.mjs', 'scripts/render-step.mjs']) {
  execFileSync(process.execPath, ['--check', new URL(path, root).pathname]);
}
const targets = new Map([['home', new Set(homeSections.map(s => s.id))], ...articles.map(a => [a.id, new Set(a.sections.map(s => s.id))])]);
for (const step of allSteps) targets.get(step.article).add(step.id);
assert.equal(targets.size, articles.length + 1, 'Article route IDs must be unique');
assert.equal(new Set(allSteps.map(s => s.id)).size, allSteps.length, 'Step IDs must be unique');
let figureCount = 0;
const imagePaths = new Set();
for (const article of articles) {
  assert.equal(new Set(article.sections.map(s => s.id)).size, article.sections.length, `Duplicate section in ${article.id}`);
  const html = article.sections.map(s => s.html).join('');
  for (const [, route, section] of html.matchAll(/href="#\/([^/"\s]+)(?:\/([^"\s]+))?"/g)) {
    assert.ok(targets.has(route), `Missing route: ${route}`);
    if (section) assert.ok(targets.get(route).has(section), `Missing section: ${route}/${section}`);
  }
  for (const [, path] of html.matchAll(/(?:src|href)="\.\/([^"#]+)"/g)) {
    await access(new URL(path, root));
    if (path.endsWith('.svg')) {
      const svg = await readFile(new URL(path, root), 'utf8');
      assert.ok(svg.includes('界面演示') && svg.includes('非线上截图') && svg.includes('<title'), `Unlabeled illustration: ${path}`);
      assert.ok(!imagePaths.has(path), `Each step needs its own image: ${path}`);
      imagePaths.add(path);
      figureCount++;
    }
  }
}
assert.equal(figureCount, allSteps.length, 'Every step must have one image');
for (const step of allSteps) {
  const article = articles.find(a => a.id === step.article);
  const section = article.sections.find(s => s.id === step.section);
  assert.ok(section, `Missing section for ${step.id}`);
  assert.ok(section.html.includes(`data-step-id="${step.id}"`), `Missing step ${step.id}`);
  const block = section.html.split(`data-step-id="${step.id}"`)[1].split('</li>')[0];
  assert.equal((block.match(/<img /g) || []).length, 1, `Expected one image for ${step.id}`);
  assert.ok(block.includes(`assets/steps/${step.id}.svg`));
  assert.ok(block.includes('step-result'), `Missing expected result for ${step.id}`);
}
for (const item of checklist) assert.ok(targets.has(item.article));
const html = await readFile(new URL('index.html', root), 'utf8');
for (const [, path] of html.matchAll(/(?:src|href)="\.\/([^"#]+)"/g)) await access(new URL(path, root));
const css = await readFile(new URL('styles.css', root), 'utf8');
for (const [, path] of css.matchAll(/url\(['"]\.\/([^'"]+)['"]\)/g)) await access(new URL(path, root));
assert.ok(css.includes('prefers-reduced-motion'));
console.log(`Passed: JS syntax, ${articles.length} articles, ${allSteps.length} steps with ${figureCount} unique images and expected results, document/step links, local assets and fonts.`);
