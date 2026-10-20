import { writeFile, mkdir, readFile } from 'node:fs/promises';
import { allSteps } from '../walkthroughs.js';
import { renderStep } from './render-step.mjs';
const root = new URL('../', import.meta.url);
const output = new URL('assets/steps/', root);
await mkdir(output, { recursive: true });
const logo = (await readFile(new URL('assets/logo.svg', root))).toString('base64');
for (const step of allSteps) {
  await writeFile(new URL(`${step.id}.svg`, output), renderStep(step, logo));
}
console.log(`Generated ${allSteps.length} individual, annotated step images.`);
