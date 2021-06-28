import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment, { Moment } from 'moment';
import { switchMap } from 'rxjs/operators';
import { BankAccount, Page, Transaction } from 'src/app/@core/models';
import { BankAccountService } from '../bank-account.service';

@UntilDestroy()
@Component({
  selector: 'app-bank-account-single',
  templateUrl: './bank-account-single.component.html',
  styleUrls: ['./bank-account-single.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class BankAccountSingleComponent implements OnInit {
  bankAccount: BankAccount;
  pageSize = 20;

  transactionsPage$: Promise<Page<Transaction>>;
  balance: number;

  private monthDate: Moment;

  constructor(private activatedRoute: ActivatedRoute, private bankAccountService: BankAccountService, private router: Router) {}

  ngOnInit(): void {
    this.activatedRoute.params
      .pipe(
        untilDestroyed(this),
        switchMap(params => {
          return this.bankAccountService.getBankAccount(params.bankAccountId);
        }),
      )
      .subscribe(bankAccount => this.getBankAccount(bankAccount));
  }

  public async getTransactions(page = 1): Promise<void> {
    this.transactionsPage$ = this.bankAccountService.getTransactions(
      this.bankAccount.id,
      { maxDate: moment(this.monthDate).endOf('month'), minDate: moment(this.monthDate).startOf('month') },
      { page, pageSize: this.pageSize },
    );
  }

  public async getBalance(): Promise<void> {
    this.balance = undefined;
    this.balance = await this.bankAccountService.getBalance(this.bankAccount.id, { maxDate: this.monthDate });
  }

  public async editTransaction(transaction: Transaction) {}

  public async deleteTransaction(transaction: Transaction) {}

  public async dateChanged(newDate: Moment): Promise<void> {
    this.monthDate = newDate;
    await this.getInfoBasedOnBank();
  }

  private async getBankAccount(bankAccount: BankAccount) {
    if (bankAccount == null) {
      this.router.navigateByUrl('/404');
      return;
    }

    this.bankAccount = bankAccount;
  }

  private getInfoBasedOnBank() {
    return Promise.all([this.getBalance(), this.getTransactions()]);
  }
}
