import { Component, OnInit } from '@angular/core';
import { TdDialogService } from '@covalent/core/dialogs';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { take } from 'rxjs/operators';
import { BankAccount } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { BankAccountDispatchers } from 'src/app/store';
import { BankAccountSelectors } from 'src/app/store/services/selectors';
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
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private bankAccountService: BankAccountService,
    private bankAccountDispatchers: BankAccountDispatchers,
    private bankAccountSelectors: BankAccountSelectors,
  ) {}

  async ngOnInit(): Promise<void> {
    this.bankAccountSelectors.bankAccounts$.pipe(untilDestroyed(this)).subscribe(bankAccounts => this.fillBankAccounts(bankAccounts));
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
        this.bankAccountDispatchers.bankAccountRemoved(bankAccount.id);
      }
    }
  }

  async changeName(bankAccount: BankAccount): Promise<void> {
    const inputName = await this.dialogService
      .openPrompt({
        title: this.translocoService.translate('change-name'),
        message: '',
        value: bankAccount.name,
        cancelButton: this.translocoService.translate('cancel'),
        acceptButton: this.translocoService.translate('edit'),
        width: '500px',
      })
      .afterClosed()
      .pipe(take(1))
      .toPromise();

    if (inputName !== bankAccount.name) {
      const nameChanged = (
        await this.bankAccountService
          .changeBankAccountName(bankAccount.id, inputName)
          .pipe(
            take(1),
            this.toast.observe({
              loading: this.translocoService.translate('editing'),
              success: this.translocoService.translate('changing-name-successful', { name: inputName }),
              error: error =>
                this.errorService.getInstantErrorMessage(error, 'changing-name-error', 'changing-name-error-with-description', {
                  oldName: bankAccount.name,
                  newName: inputName,
                }),
            }),
          )
          .toPromise()
      )?.name;

      if (nameChanged != null && nameChanged !== bankAccount.name) {
        this.bankAccountDispatchers.bankAccountNameChanged(bankAccount.id, nameChanged);
      }
    }
  }

  private fillBankAccounts(bankAccounts: BankAccount[]): void {
    this.bankAccounts = [...(bankAccounts || [])].sort((bankAccountA, bankAccountB) => bankAccountA.name.localeCompare(bankAccountB.name));
  }
}
