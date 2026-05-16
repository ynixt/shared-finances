import { describe, expect, it } from 'vitest';

import { getTokenExpirationEpochMs, parseJwtPayload } from './jwt-token.util';

describe('jwt-token util', () => {
  it('parses exp from a base64url jwt payload', () => {
    const exp = 1_800_000_000;
    const token = buildToken({ exp, sub: 'user-1' });

    expect(parseJwtPayload(token)).toEqual({
      exp,
      sub: 'user-1',
    });
    expect(getTokenExpirationEpochMs(token)).toBe(exp * 1000);
  });

  it('returns null for malformed tokens', () => {
    expect(parseJwtPayload('not-a-jwt')).toBeNull();
    expect(getTokenExpirationEpochMs('not-a-jwt')).toBeNull();
  });
});

function buildToken(payload: Record<string, unknown>): string {
  const header = { alg: 'HS256', typ: 'JWT' };

  return `${encodeBase64Url(header)}.${encodeBase64Url(payload)}.signature`;
}

function encodeBase64Url(value: Record<string, unknown>): string {
  return btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}
