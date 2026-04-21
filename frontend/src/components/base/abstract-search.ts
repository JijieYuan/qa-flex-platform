import { pinyin } from 'pinyin-pro';

export interface AbstractSearchIndex {
  normalized: string;
  compact: string;
  spell: string;
  initials: string;
}

function normalizeText(text: string) {
  return text.trim().toLowerCase();
}

function compactText(text: string) {
  return normalizeText(text).replace(/\s+/g, '');
}

function isChineseChar(char: string) {
  return /[\u3400-\u9fff]/.test(char);
}

function isAsciiWordChar(char: string) {
  return /[a-z0-9]/i.test(char);
}

function tokenizeText(text: string) {
  const tokens: string[] = [];
  let asciiBuffer = '';

  for (const char of text) {
    if (isAsciiWordChar(char)) {
      asciiBuffer += char.toLowerCase();
      continue;
    }
    if (asciiBuffer) {
      tokens.push(asciiBuffer);
      asciiBuffer = '';
    }
    if (isChineseChar(char)) {
      tokens.push(char);
    }
  }

  if (asciiBuffer) {
    tokens.push(asciiBuffer);
  }

  return tokens;
}

function tokenSpell(token: string) {
  if (!token) {
    return '';
  }
  if (/^[a-z0-9]+$/i.test(token)) {
    return token.toLowerCase();
  }
  return pinyin(token, { toneType: 'none', type: 'string' }).replace(/\s+/g, '').toLowerCase();
}

function tokenInitial(token: string) {
  if (!token) {
    return '';
  }
  if (/^[a-z0-9]+$/i.test(token)) {
    return token[0]?.toLowerCase() ?? '';
  }
  return pinyin(token, { pattern: 'first', toneType: 'none', type: 'string' }).replace(/\s+/g, '').toLowerCase();
}

export function buildAbstractSearchIndex(text: string): AbstractSearchIndex {
  const tokens = tokenizeText(text);
  return {
    normalized: normalizeText(text),
    compact: compactText(text),
    spell: tokens.map(tokenSpell).join(''),
    initials: tokens.map(tokenInitial).join(''),
  };
}

export function matchesAbstractSearchText(text: string, query: string) {
  const haystack = buildAbstractSearchIndex(text);
  const queryIndex = buildAbstractSearchIndex(query);
  const candidates = [...new Set([queryIndex.normalized, queryIndex.compact, queryIndex.spell].filter(Boolean))];

  if (!candidates.length) {
    return true;
  }

  return candidates.some(
    (candidate) =>
      haystack.normalized.includes(candidate) ||
      haystack.compact.includes(candidate) ||
      haystack.spell.includes(candidate) ||
      haystack.initials.includes(candidate),
  );
}
