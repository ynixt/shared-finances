import { Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';

import { NewEntryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { ErrorMessageService } from '../../../../services/error-message.service';
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

  submitting = false;
  async submit(request: NewEntryDto) {
    if (this.submitting) {
      return;
    }

    this.submitting = true;

    try {
      await this.walletEntryService.createWalletEntry(request);
      // await this.router.navigate(['..'], { relativeTo: this.route });
      this.submitting = false; // TODO
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
      this.submitting = false;
      throw error;
    }
  }
}
