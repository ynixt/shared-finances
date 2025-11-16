import { Injectable } from '@angular/core';

import dayjs from 'dayjs';

import { skipWeekend } from '../../../util/date-util';

@Injectable({ providedIn: 'root' })
export class CreditCardBillService {
  getBestBill(transactionDate: Date, dueDay: number, dueOnNextBusinessDay: boolean): Date {
    let date = dayjs(transactionDate).startOf('month').date(dueDay);

    if (dueOnNextBusinessDay) {
      date = skipWeekend(date);
    }

    if (dayjs(transactionDate).isAfter(date)) {
      date = date.startOf('month').add(1, 'month');
    }

    return date.startOf('month').toDate();
  }
}
