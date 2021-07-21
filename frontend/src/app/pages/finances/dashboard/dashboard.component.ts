import { Component, OnInit } from '@angular/core';
import { Moment } from 'moment';
import { Observable } from 'rxjs';
import { BankAccountSummary, User } from 'src/app/@core/models';
import { BankAccountService } from 'src/app/@core/services';
import { AuthSelectors } from 'src/app/store/services/selectors';

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

  constructor(private bankAccountService: BankAccountService) {}

  ngOnInit(): void {}

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

  private getMaxDate(disallowFutureOnSameMonth = true) {
    return this.bankAccountService.getMaxDate(this.monthDate, disallowFutureOnSameMonth);
  }

  private getInfoBasedOnDate(): Promise<any> {
    return Promise.all([this.getBankAccountSummary()]);
  }
}
