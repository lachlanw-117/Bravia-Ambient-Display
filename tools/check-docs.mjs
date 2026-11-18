import { readFileSync, existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Deliberately local: no URL requests, package dependencies or runtime data reads.
export function markdownInfo(text) {
  const lines = text.replace(/^\uFEFF/, '').split(/\r?\n/);
  const anchors = new Set(), counts = new Map(), links = [];
  let fence = null, comment = false;
  for (let index = 0; index < lines.length; index++) {
    let line = lines[index];
    const delimiter = line.match(/^\s{0,3}(`{3,}|~{3,})/);
    if (delimiter) {
      if (!fence) fence = delimiter[1];
      else if (delimiter[1][0] === fence[0] && delimiter[1].length >= fence.length &&
        line.trim() === delimiter[1]) fence = null;
      continue;
    }
    if (fence) continue;
    // Comments may span lines; leave visible text on either side intact.
    let visible = '';
    while (line.length) {
      const position = line.indexOf(comment ? '-->' : '<!--');
      if (position < 0) { if (!comment) visible += line; break; }
      if (!comment) visible += line.slice(0, position);
      line = line.slice(position + (comment ? 3 : 4)); comment = !comment;
    }
    line = visible;
    const heading = line.match(/^\s{0,3}#{1,6}\s+(.+?)(?:\s+#+)?\s*$/);
    if (heading) {
      const base = heading[1].replace(/\[([^\]]+)\]\([^)]*\)/g, '$1').replace(/<[^>]*>/g, '')
        .replace(/[`*_~]/g, '').toLowerCase().replace(/[^\p{L}\p{N}_\-\s]/gu, '').replace(/\s/g, '-');
      let slug = base, suffix = counts.get(base) ?? 0;
      while (anchors.has(slug)) slug = `${base}-${++suffix}`;
      counts.set(base, suffix); anchors.add(slug);
    }
    for (const match of line.matchAll(/<a\s[^>]*(?:id|name)=["']([^"']+)["'][^>]*>/g)) anchors.add(match[1]);
    line = line.replace(/(`+)[^\n]*?\1/g, '');
    for (const match of line.matchAll(/!?\[[^\]]*\]\(\s*(<[^>]+>|[^\s)]+)(?:\s+["'][^"']*["'])?\s*\)/g))
      links.push({ target: match[1].replace(/^<|>$/g, ''), line: index + 1 });
    const reference = line.match(/^\s{0,3}\[[^\]]+\]:\s*(<[^>]+>|\S+)/);
    if (reference) links.push({ target: reference[1].replace(/^<|>$/g, ''), line: index + 1 });
  }
  return { anchors, links };
}

export function checkDocuments(root, files) {
  const errors = [], parsed = new Map();
  root = path.resolve(root);
  const info = file => {
    if (!parsed.has(file)) parsed.set(file, markdownInfo(readFileSync(file, 'utf8')));
    return parsed.get(file);
  };
  for (const file of files) {
    const absolute = path.resolve(root, file);
    for (const { target, line } of info(absolute).links) {
      if (/^[a-z][a-z0-9+.-]*:/i.test(target) || target.startsWith('//')) continue;
      const label = `${file}:${line}: ${target}`;
      try {
        const hash = target.indexOf('#');
        const rawPath = (hash < 0 ? target : target.slice(0, hash)).split('?')[0];
        const destination = rawPath ? path.resolve(path.dirname(absolute), decodeURIComponent(rawPath)) : absolute;
        const relative = path.relative(root, destination);
        if (relative === '..' || relative.startsWith('..' + path.sep) || path.isAbsolute(relative)) {
          errors.push(`${label}: outside repository`); continue;
        }
        if (!existsSync(destination)) { errors.push(`${label}: missing file`); continue; }
        if (hash >= 0 && /\.md$/i.test(destination)) {
          const fragment = decodeURIComponent(target.slice(hash + 1));
          if (fragment && !info(destination).anchors.has(fragment)) errors.push(`${label}: missing heading/anchor`);
        }
      } catch (error) { errors.push(`${label}: ${error instanceof URIError ? 'invalid URL encoding' : 'cannot read target'}`); }
    }
  }
  return errors;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
  const list = args => execFileSync('git', ['ls-files', '-z', ...args], { cwd: root, encoding: 'utf8' }).split('\0').filter(Boolean);
  // Include new authored docs before staging, but never sweep unrelated local notes.
  const files = [...new Set([...list(['--cached', '--', '*.md']),
    ...list(['--others', '--exclude-standard', '--', 'docs/', '.github/']).filter(file => file.endsWith('.md'))])];
  const errors = checkDocuments(root, files);
  if (errors.length) { console.error(errors.join('\n')); process.exitCode = 1; }
  else console.log(`Local links and Markdown anchors passed: ${files.length} documents (external URLs not requested).`);
}
