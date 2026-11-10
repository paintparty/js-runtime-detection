// Runs the :advanced worker probe in workerd, Cloudflare's Workers runtime,
// through Miniflare, and compares the detected values against the Cloudflare
// Workers row of the detection matrix.
// Usage: node test/cloudflare/run.mjs <probe.js>

import { readFileSync } from 'node:fs';
import { Miniflare } from 'miniflare';

const probe = readFileSync(process.argv[2], 'utf8');
const script = `${probe}
export default { fetch() { return new Response(globalThis.__jsRuntimeDetect); } };`;

// Pinned so results don't change when Miniflare's default date moves. On
// 2025-09-15 and later, nodejs_compat provides process.versions.node and
// process.release.name = "node".
const compatibilityDate = '2026-07-01';

const row = {
  'js-runtime':                 'workerd',
  'js-platform':                'cloudflare',
  'js-runtime-is-server':         true,
  'js-runtime-has-dom':        false,
  'js-runtime-has-jsdom':      false,
  'js-runtime-has-node-apis':  false,
  'js-runtime-is-strict-node': false,
};

const cases = [
  { name: 'Cloudflare Workers', flags: [], expected: row },
  { name: 'Cloudflare Workers + nodejs_compat', flags: ['nodejs_compat'],
    expected: { ...row, 'js-runtime-has-node-apis': true } },
];

let failed = 0;
for (const { name, flags, expected } of cases) {
  const mf = new Miniflare({ modules: true, script, compatibilityDate, compatibilityFlags: flags });
  try {
    const actual = await (await mf.dispatchFetch('http://localhost/')).json();
    const ok = Object.keys(expected).every((k) => actual[k] === expected[k]);
    console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}`);
    if (!ok) {
      console.log(`  expected: ${JSON.stringify(expected)}\n  actual:   ${JSON.stringify(actual)}`);
      failed += 1;
    }
  } finally {
    await mf.dispose();
  }
}
process.exitCode = failed ? 1 : 0;
