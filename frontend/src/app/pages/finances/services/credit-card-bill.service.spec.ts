import { TestBed } from '@angular/core/testing';

import dayjs from 'dayjs';
import { beforeEach, describe, expect, it } from 'vitest';

import { ONLY_DATE_FORMAT } from '../../../util/date-util';
import { CreditCardBillService } from './credit-card-bill.service';

describe('getBestBill', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({}).compileComponents();
  });

  it('The bill of the current month is already overdue. Should use next month', () => {
    const service = TestBed.inject(CreditCardBillService);

    const transactionDate = dayjs().year(2026).month(1).date(1); //2026-02-01
    const dueDay = 1;
    const dueOnNextBusinessDay = true;
    const daysBetweenDueAndClosing = 10;

    const result = service.getBestBill(transactionDate, dueDay, dueOnNextBusinessDay, daysBetweenDueAndClosing);

    const expected = dayjs().year(2026).month(2).date(1); //2026-03-01

    expect(result.format(ONLY_DATE_FORMAT)).toBe(expected.format(ONLY_DATE_FORMAT));
  });

  it('The bill of the current month is open. Should use it', () => {
    const service = TestBed.inject(CreditCardBillService);

    const transactionDate = dayjs().year(2026).month(1).date(2); //2026-02-02
    const dueDay = 10;
    const dueOnNextBusinessDay = true;
    const daysBetweenDueAndClosing = 6;

    const result = service.getBestBill(transactionDate, dueDay, dueOnNextBusinessDay, daysBetweenDueAndClosing);

    const expected = dayjs().year(2026).month(1).date(1); //2026-02-01

    expect(result.format(ONLY_DATE_FORMAT)).toBe(expected.format(ONLY_DATE_FORMAT));
  });

  it('The bill of the current month is closed. Should use next month', () => {
    const service = TestBed.inject(CreditCardBillService);

    const transactionDate = dayjs().year(2026).month(1).date(4); //2026-02-04
    const dueDay = 10;
    const dueOnNextBusinessDay = true;
    const daysBetweenDueAndClosing = 7;
    // closing is 2026-02-03

    const result = service.getBestBill(transactionDate, dueDay, dueOnNextBusinessDay, daysBetweenDueAndClosing);

    const expected = dayjs().year(2026).month(2).date(1); //2026-03-01

    expect(result.format(ONLY_DATE_FORMAT)).toBe(expected.format(ONLY_DATE_FORMAT));
  });
});
