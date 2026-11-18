import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, writeFileSync, mkdirSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { checkDocuments, markdownInfo } from '../check-docs.mjs';

function fixture(t, source) {
  const root = mkdtempSync(path.join(tmpdir(), 'bravia-docs-'));
  t.after(() => {
    const resolved = path.resolve(root), prefix = path.resolve(tmpdir()) + path.sep + 'bravia-docs-';
    if (!resolved.startsWith(prefix)) throw Error('Refusing cleanup outside the named test temporary directory');
    rmSync(resolved, { recursive: true, force: true });
  });
  mkdirSync(path.join(root, 'docs'));
  writeFileSync(path.join(root, 'README.md'), source);
  writeFileSync(path.join(root, 'docs', 'Guide one.md'), '# Setup\n\n## Cache `v1`\n\n## Cache `v1`\n');
  return root;
}

test('checks relative paths, encoded names, self links and duplicate-heading slugs', t => {
  const root = fixture(t, '# Overview\n[Self](#overview)\n[Guide](docs/Guide%20one.md#cache-v1-1)\n[Space](<docs/Guide one.md#setup>)\n');
  assert.deepEqual(checkDocuments(root, ['README.md']), []);
});
test('reports missing files and anchors with source locations', t => {
  const root = fixture(t, '[Missing](absent.md)\n[Wrong](docs/Guide%20one.md#no-such-heading)\n');
  const errors = checkDocuments(root, ['README.md']);
  assert.equal(errors.length, 2);assert.match(errors[0], /README.md:1:.*missing file/);
  assert.match(errors[1], /README.md:2:.*missing heading\/anchor/);
});
test('ignores fenced examples, inline code, comments and external URLs', t => {
  const root = fixture(t, '# Top\n```md\n[Example](missing.md)\n```\n~~~\n[Example](missing.md)\n~~~\n`[inline](missing.md)`\n<!--\n[hidden](missing.md)\n-->\n[Web](https://example.invalid/path#x)\n[Mail](mailto:example@example.invalid)\n');
  assert.deepEqual(checkDocuments(root, ['README.md']), []);
});
test('validates image targets, reference definitions and explicit anchors', t => {
  const root = fixture(t, '<a id="custom"></a>\n[Go](#custom)\n[guide]: docs/Guide%20one.md#setup\n![Missing](missing.svg)\n');
  const errors = checkDocuments(root, ['README.md']);assert.equal(errors.length, 1);assert.match(errors[0], /missing.svg/);
});
test('rejects paths escaping the repository and malformed URI encoding', t => {
  const root = fixture(t, '[Outside](../outside.md)\n[Bad](bad%ZZ.md)\n');
  const errors = checkDocuments(root, ['README.md']);assert.equal(errors.length, 2);
  assert.match(errors[0], /outside repository/);assert.match(errors[1], /invalid URL encoding/);
});
test('heading collisions and punctuation follow GitHub-style anchors', () => {
  const info = markdownInfo('# A: `test`\n# A: `test`\n# A test-1\n# A: `test`\n');
  assert.deepEqual([...info.anchors], ['a-test','a-test-1','a-test-1-1','a-test-2']);
});
