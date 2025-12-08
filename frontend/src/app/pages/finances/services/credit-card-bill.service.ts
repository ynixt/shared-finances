import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import dayjs from 'dayjs';

import { CreditCardBillDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/creditCard';
import { UserService } from '../../../services/user.service';
import { ONLY_DATE_FORMAT, skipWeekend } from '../../../util/date-util';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({ providedIn: 'root' })
export class CreditCardBillService {
  private readonly httpClient = inject(HttpClient);
  private readonly userService = inject(UserService);

  getBestBill(
    transactionDate: Date | dayjs.Dayjs,
    dueDay: number,
    dueOnNextBusinessDay: boolean,
    daysBetweenDueAndClosing: number,
  ): dayjs.Dayjs {
    let date = dayjs(transactionDate).startOf('month').date(dueDay);

    if (dueOnNextBusinessDay) {
      date = skipWeekend(date);
    }

    date = date.subtract(daysBetweenDueAndClosing, 'day');

    if (dayjs(transactionDate).isAfter(date)) {
      date = date.startOf('month').add(1, 'month');
    }

    return date.startOf('month');
  }

  async getBillForMonth(creditCardId: string, month: number, year: number): Promise<CreditCardBillDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(
        this.httpClient.get<CreditCardBillDto>(`/api/credit-card-bills/${creditCardId}/of/${year}/${month}`).pipe(take(1)),
      );
    }

    throw new UserMissingError();
  }

  async changeClosingDate(creditCardId: string, closingDate: dayjs.Dayjs): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(
        this.httpClient
          .put<void>(`/api/credit-card-bills/${creditCardId}/closingDate/${closingDate.format(ONLY_DATE_FORMAT)}`, undefined)
          .pipe(take(1)),
      );
      return;
    }

    throw new UserMissingError();
  }

  async changeDueDate(creditCardId: string, dueDate: dayjs.Dayjs): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(
        this.httpClient
          .put<void>(`/api/credit-card-bills/${creditCardId}/dueDate/${dueDate.format(ONLY_DATE_FORMAT)}`, undefined)
          .pipe(take(1)),
      );
      return;
    }

    throw new UserMissingError();
  }
}
