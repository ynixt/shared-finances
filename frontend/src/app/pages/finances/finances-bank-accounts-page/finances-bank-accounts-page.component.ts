import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowUpRightFromSquare, faPenToSquare } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

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
export class FinancesBankAccountsPageComponent {
  readonly editFinanceIcon = faPenToSquare;
  readonly openFinanceIcon = faArrowUpRightFromSquare;
  readonly pageSize = 12;

  loading = true;
  bankAccounts: Page<BankAccountDto> = createEmptyPage();

  constructor(private bankAccountService: BankAccountService) {}

  private async loadPage(page = 0) {
    this.loading = true;

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
}
