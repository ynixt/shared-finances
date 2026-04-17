import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';

import { NewEntryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { DEFAULT_ERROR_LIFE } from '../../../../util/error-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { WalletEntryService } from '../../services/wallet-entry.service';
import { TransactionFormComponent } from '../shared/transaction-form/transaction-form.component';

@Component({
  selector: 'app-new-transaction-page',
  imports: [FinancesTitleBarComponent, TranslatePipe, TransactionFormComponent],
  templateUrl: './new-transaction-page.component.html',
  styleUrl: './new-transaction-page.component.scss',
})
export class NewTransactionPageComponent {
  private readonly walletEntryService = inject(WalletEntryService);
  private readonly messageService = inject(MessageService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly router = inject(Router);
  private readonly translateService = inject(TranslateService);

  submitting = false;
  async submit(request: NewEntryDto) {
    if (this.submitting) {
      return;
    }

    this.submitting = true;

    try {
      await this.walletEntryService.createWalletEntry(request);
      await this.submitSuccess(request);
      this.submitting = false; // TODO
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
      this.submitting = false;
      throw error;
    }
  }

  private async submitSuccess(request: NewEntryDto) {
    this.messageService.add({
      severity: 'success',
      summary: this.translateService.instant('general.success'),
      detail: this.translateService.instant('financesPage.transactionsPage.newTransactionPage.success'),
      life: DEFAULT_ERROR_LIFE,
    });

    if (request.groupId == null) {
      await this.router.navigate(['/app']);
    } else {
      await this.router.navigate(['/app', 'groups', request.groupId]);
    }
  }
}
