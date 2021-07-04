import { forwardRef, Inject, Injectable } from '@nestjs/common';
import { AuthenticationError } from 'apollo-server-express';
import moment from 'moment-timezone';
import { FBUser } from '../auth/firebase-strategy';
import { BankAccountService } from '../bank-account';
import { Chart, ChartSerie } from '../models';
import { TransactionRepository } from './transaction.repository';

@Injectable()
export class TransactionChartService {
  constructor(
    private transacationRepository: TransactionRepository,
    @Inject(forwardRef(() => BankAccountService)) private bankAccountService: BankAccountService,
  ) {}

  async getChartByBankAccountId(
    user: FBUser,
    bankAccountId: string,
    timezone: string,
    args?: { minDate?: string; maxDate?: string },
  ): Promise<Chart[]> {
    if (!(await this.bankAccountService.existsWithUserId(user.id, bankAccountId))) {
      throw new AuthenticationError('');
    }

    const data = (await this.transacationRepository.getByBankAccountIdGroupedByDate(bankAccountId, timezone, args)) ?? [];

    const series = data
      .map(data => new ChartSerie({ name: this.convertMonthYearToName(data._id.month, data._id.year, timezone), value: data.balance }))
      .sort((serieA, serieB) => serieA.name.localeCompare(serieB.name));

    series.forEach((serie, index) => {
      if (index > 0) {
        serie.value += series[index - 1].value;
      }
    });

    return [new Chart({ name: bankAccountId, series })];
  }

  private convertMonthYearToName(month: number, year: number, timeZone: string) {
    const month2Digits = month < 10 ? `0${month}` : month.toString();
    return moment.tz(`${year}-${month2Digits}-01 00:00`, timeZone).toISOString();
  }
}
