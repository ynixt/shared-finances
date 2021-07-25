import { Component, OnInit } from '@angular/core';
import { Moment } from 'moment';
import { merge } from 'rxjs';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import { BankAccountSummary } from 'src/app/@core/models';
import { BankAccountService, TransactionService } from 'src/app/@core/services';

@UntilDestroy()
@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  bankAccountSummaryState: { isLoading: boolean; summary?: BankAccountSummary } = {
    isLoading: true,
  };

  private monthDate: Moment;

  constructor(private bankAccountService: BankAccountService, private transactionService: TransactionService) {}

  ngOnInit(): void {
    this.transactionsChangeObserver();
  }

  public async dateChanged(newDate: Moment): Promise<void> {
    this.monthDate = newDate;
    await this.getInfoBasedOnDate();
  }

  public async getBankAccountSummary(): Promise<void> {
    const maxDate = this.getMaxDate();

    this.bankAccountSummaryState.isLoading = true;
    this.bankAccountSummaryState.summary = await this.bankAccountService.getBankAccountSummary({ maxDate: maxDate });
    this.bankAccountSummaryState.isLoading = false;
  }

  private transactionsChangeObserver(): void {
    merge(
      this.bankAccountService.onTransactionCreated(),
      this.bankAccountService.onTransactionUpdated(),
      this.bankAccountService.onTransactionDeleted(),
    )
      .pipe(untilDestroyed(this))
      .subscribe(async () => {
        await Promise.all([this.getBankAccountSummary()]);
      });
  }

  private getMaxDate(disallowFutureOnSameMonth = true) {
    return this.transactionService.getMaxDate(this.monthDate, disallowFutureOnSameMonth);
  }

  private getInfoBasedOnDate(): Promise<any> {
    return Promise.all([this.getBankAccountSummary()]);
  }
}
