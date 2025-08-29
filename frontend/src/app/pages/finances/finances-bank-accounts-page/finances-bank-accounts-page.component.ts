import { Component, OnInit } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { BankAccountDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/bankAccount';
import { Page } from '../../../models/pagination';
import { createEmptyPage } from '../../../services/pagination.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { BankAccountService } from '../services/bank-account.service';

@Component({
  selector: 'app-finances-bank-accounts-page',
  imports: [TranslatePipe, FinancesTitleBarComponent],
  templateUrl: './finances-bank-accounts-page.component.html',
  styleUrl: './finances-bank-accounts-page.component.scss',
})
export class FinancesBankAccountsPageComponent implements OnInit {
  loading = true;
  bankAccounts: Page<BankAccountDto> = createEmptyPage();

  constructor(private bankAccountService: BankAccountService) {}

  async ngOnInit(): Promise<void> {
    await this.loadPage();
  }

  private async loadPage() {
    this.loading = true;

    this.bankAccounts = await this.bankAccountService.getAllBankAccount();

    this.loading = false;
  }
}
