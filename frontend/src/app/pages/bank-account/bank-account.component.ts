import { Component, OnInit } from '@angular/core';
import { TdDialogService } from '@covalent/core/dialogs';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { take } from 'rxjs/operators';
import { BankAccount, User } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { AuthDispatchers } from 'src/app/store';
import { AuthSelectors } from 'src/app/store/services/selectors';
import { BankAccountService } from './bank-account.service';

@UntilDestroy()
@Component({
  selector: 'app-bank-account',
  templateUrl: './bank-account.component.html',
  styleUrls: ['./bank-account.component.scss'],
})
export class BankAccountComponent implements OnInit {
  bankAccounts: BankAccount[] = [];

  constructor(
    private authSelector: AuthSelectors,
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private authDispatchers: AuthDispatchers,
    private authSelectors: AuthSelectors,
    private bankAccountService: BankAccountService,
  ) {}

  async ngOnInit(): Promise<void> {
    this.authSelector.user$.pipe(untilDestroyed(this)).subscribe(user => this.fillBankAccounts(user));
  }

  async delete(bankAccount: BankAccount): Promise<void> {
    const confirm = await this.dialogService
      .openConfirm({
        title: this.translocoService.translate('confirm'),
        message: this.translocoService.translate('delete-confirm', { name: bankAccount.name }),
        cancelButton: this.translocoService.translate('cancel'),
        acceptButton: this.translocoService.translate('delete'),
        width: '500px',
      })
      .afterClosed()
      .pipe(take(1))
      .toPromise();

    if (confirm) {
      this.toast.observe;

      const removed = await this.bankAccountService
        .deleteBankAccount(bankAccount.id)
        .pipe(
          take(1),
          this.toast.observe({
            loading: this.translocoService.translate('deleting'),
            success: this.translocoService.translate('deleting-successful', { name: bankAccount.name }),
            error: error =>
              this.errorService.getInstantErrorMessage(error, 'deleting-error', 'deleting-error-with-description', {
                name: bankAccount.name,
              }),
          }),
        )
        .toPromise();

      if (removed) {
        await this.applyBankAccountRemovedOnUnser(bankAccount);
      }
    }
  }

  private async applyBankAccountRemovedOnUnser(bankAccountRemoved: BankAccount): Promise<void> {
    const currentUser: User = { bankAccounts: [], ...(await this.authSelectors.currentUser()) };

    currentUser.bankAccounts = currentUser.bankAccounts.filter(creditCard => creditCard.id !== bankAccountRemoved.id);

    this.authDispatchers.userUpdated(currentUser);
  }

  private fillBankAccounts(user: User): void {
    this.bankAccounts = [...(user.bankAccounts || [])].sort((bankAccountA, bankAccountB) =>
      bankAccountA.name.localeCompare(bankAccountB.name),
    );
  }
}
