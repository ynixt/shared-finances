import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowUpRightFromSquare, faPenToSquare } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { throttleTime } from 'rxjs';

import { ButtonDirective } from 'primeng/button';
import { Card } from 'primeng/card';
import { DataView, DataViewLazyLoadEvent } from 'primeng/dataview';
import { Tooltip } from 'primeng/tooltip';

import { BankAccountDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/bankAccount';
import { Page } from '../../../models/pagination';
import { LocalCurrencyPipe } from '../../../pipes/local-currency.pipe';
import { createEmptyPage } from '../../../services/pagination.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { BankAccountService } from '../services/bank-account.service';
import { UserActionEventService } from '../services/user-action-event.service';

@Component({
  selector: 'app-finances-bank-accounts-page',
  imports: [
    TranslatePipe,
    FinancesTitleBarComponent,
    DataView,
    Card,
    LocalCurrencyPipe,
    ButtonDirective,
    FaIconComponent,
    RouterLink,
    Tooltip,
  ],
  templateUrl: './finances-bank-accounts-page.component.html',
  styleUrl: './finances-bank-accounts-page.component.scss',
})
@UntilDestroy()
export class FinancesBankAccountsPageComponent {
  readonly editFinanceIcon = faPenToSquare;
  readonly openFinanceIcon = faArrowUpRightFromSquare;
  readonly pageSize = 12;

  loading = true;
  bankAccounts: Page<BankAccountDto> = createEmptyPage();

  private currentPage = 0;

  constructor(
    private bankAccountService: BankAccountService,
    userActionEventService: UserActionEventService,
  ) {
    userActionEventService.bankInserted$.pipe(untilDestroyed(this), throttleTime(100)).subscribe(this.bankInserted.bind(this));
    userActionEventService.bankUpdated$.pipe(untilDestroyed(this)).subscribe(this.bankUpdated.bind(this));
    userActionEventService.bankDeleted$.pipe(untilDestroyed(this)).subscribe(this.bankDeleted.bind(this));
  }

  private async loadPage(page = 0) {
    this.loading = true;
    this.currentPage = page;

    this.bankAccounts = await this.bankAccountService.getAllBankAccount({
      page,
      size: this.pageSize,
      sort: [
        {
          property: 'name',
          direction: 'ASC',
        },
      ],
    });

    this.loading = false;
  }

  onLazyLoad(event: DataViewLazyLoadEvent) {
    const newPage = this.bankAccounts.totalPages == 0 ? 0 : Math.floor(event.first / this.bankAccounts.totalPages);
    this.loadPage(newPage);
  }

  private bankInserted(_: BankAccountDto) {
    if (this.currentPage === 0) {
      this.loadPage();
    }
  }

  private bankUpdated(bankAccountDto: BankAccountDto) {
    const index = this.bankAccounts.content.findIndex(b => b.id === bankAccountDto.id);

    if (index != -1) {
      const newItems = [...this.bankAccounts.content];
      newItems[index] = bankAccountDto;

      this.bankAccounts = {
        ...this.bankAccounts,
        content: newItems,
      };
    }
  }

  private bankDeleted(id: string) {
    const index = this.bankAccounts.content.findIndex(b => b.id === id);

    if (index != -1) {
      const newItems = [...this.bankAccounts.content];
      newItems.splice(index, 1);

      this.bankAccounts = {
        ...this.bankAccounts,
        content: newItems,
      };
    }
  }
}
