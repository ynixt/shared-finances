import dayjs from 'dayjs';
import { describe, expect, it } from 'vitest';

import { createMonthDateRange, readDateRangeFromQueryParams } from './date-query-params.util';

describe('date-query-params.util', () => {
  it('reads yyyy-mm month query params', () => {
    const result = readDateRangeFromQueryParams(queryParamsOf({ date: '2026-04' }), 'normal');

    expect(result?.startDate.format('YYYY-MM-DD')).toBe('2026-04-01');
    expect(result?.endDate.format('YYYY-MM-DD')).toBe('2026-04-30');
    expect(result?.sameMonth).toBe(true);
  });

  it('keeps backward compatibility with legacy mm-yyyy month query params', () => {
    const result = readDateRangeFromQueryParams(queryParamsOf({ date: '04-2026' }), 'normal');

    expect(result?.startDate.format('YYYY-MM-DD')).toBe('2026-04-01');
    expect(result?.endDate.format('YYYY-MM-DD')).toBe('2026-04-30');
    expect(result?.sameMonth).toBe(true);
  });

  it('creates month ranges in yyyy-mm format', () => {
    const range = createMonthDateRange(dayjs('2026-06-18'), 'day_only');

    expect(range.startDate.format('YYYY-MM')).toBe('2026-06');
  });
});

function queryParamsOf(values: Record<string, string>): { get: (key: string) => string | null } {
  return {
    get: key => values[key] ?? null,
  };
}
