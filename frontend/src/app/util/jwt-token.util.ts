export type JwtPayload = {
  exp?: number;
};

export function parseJwtPayload(token: string): JwtPayload | null {
  const parts = token.split('.');

  if (parts.length < 2) {
    return null;
  }

  try {
    const normalizedPayload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '=');
    const json = atob(paddedPayload);
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

export function getTokenExpirationEpochMs(token: string): number | null {
  const exp = parseJwtPayload(token)?.exp;

  if (typeof exp !== 'number' || !Number.isFinite(exp)) {
    return null;
  }

  return Math.trunc(exp * 1000);
}
