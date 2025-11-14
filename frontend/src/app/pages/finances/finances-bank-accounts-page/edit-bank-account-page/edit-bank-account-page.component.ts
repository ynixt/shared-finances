import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { InputText } from 'primeng/inputtext';
import { ProgressSpinner } from 'primeng/progressspinner';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { CurrencySelectorComponent } from '../../../../components/currency-selector/currency-selector.component';
import { RequiredFieldAsteriskComponent } from '../../../../components/required-field-asterisk/required-field-asterisk.component';
import { BankAccountDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/bankAccount';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { DEFAULT_ERROR_LIFE } from '../../../../util/error-util';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { BankAccountService } from '../../services/bank-account.service';

@Component({
  selector: 'app-edit-bank-account-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ProgressSpinner,
    ButtonDirective,
    CurrencySelectorComponent,
    FormsModule,
    InputText,
    ReactiveFormsModule,
    ToggleSwitch,
    ConfirmDialog,
    RequiredFieldAsteriskComponent,
  ],
  templateUrl: './edit-bank-account-page.component.html',
  styleUrl: './edit-bank-account-page.component.scss',
  providers: [ConfirmationService],
})
@UntilDestroy()
export class EditBankAccountPageComponent {
  bankAccount: BankAccountDto | null = null;
  loading = true;
  submitting = false;
  formGroup: FormGroup | undefined = undefined;
  currency: string = 'USD';

  constructor(
    private route: ActivatedRoute,
    private bankAccountService: BankAccountService,
    private router: Router,
    private fb: FormBuilder,
    private messageService: MessageService,
    private translateService: TranslateService,
    private confirmationService: ConfirmationService,
    private errorMessageService: ErrorMessageService,
  ) {
    this.route.paramMap.pipe(untilDestroyed(this)).subscribe(params => {
      const id = params.get('id');

      if (id) {
        this.getBankAccount(id);
      } else {
        this.goToNotFound();
      }
    });
  }

  async submit() {
    if (this.formGroup == null || this.bankAccount == null || this.formGroup.invalid || this.submitting) {
      return;
    }

    this.submitting = true;

    try {
      await this.bankAccountService.editBankAccount(this.bankAccount.id, {
        newName: this.formGroup.value.name,
        newEnabled: this.formGroup.value.enabled,
        newCurrency: this.formGroup.value.currency,
      });

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.bankAccountsPage.editBankAccountPage.successMessage'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await this.router.navigate(['../..'], { relativeTo: this.route });
    } catch (err) {
      this.submitting = false;

      this.messageService.add({
        severity: 'error',
        summary: this.translateService.instant('error.genericTitle'),
        detail: this.translateService.instant('error.genericMessage'),
        life: DEFAULT_ERROR_LIFE,
      });
    }
  }

  async askForConfirmationToDelete() {
    this.confirmationService.confirm({
      message: this.translateService.instant('general.genericConfirmation'),
      header: this.translateService.instant('general.confirmation'),
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('general.delete'),
      rejectLabel: this.translateService.instant('general.cancel'),
      acceptButtonProps: {
        severity: 'danger',
      },
      rejectButtonProps: {
        severity: 'secondary',
      },
      accept: () => {
        this.deleteBankAccount();
      },
    });
  }

  private async deleteBankAccount() {
    if (this.bankAccount == null || this.submitting) return;

    this.submitting = true;

    try {
      await this.bankAccountService.deleteBankAccount(this.bankAccount.id);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.bankAccountsPage.editBankAccountPage.successDeleteMessage'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await this.router.navigate(['../..'], { relativeTo: this.route });
    } catch (err) {
      this.submitting = false;

      this.messageService.add({
        severity: 'error',
        summary: this.translateService.instant('error.genericTitle'),
        detail: this.translateService.instant('error.genericMessage'),
        life: DEFAULT_ERROR_LIFE,
      });

      console.error(err);
    }
  }

  private async getBankAccount(id: string): Promise<void> {
    this.loading = true;

    try {
      this.bankAccount = await this.bankAccountService.getBankAccount(id);
      this.createForm();
      this.loading = false;
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 404 || error.status === 400) {
          await this.goToNotFound();
          return;
        }
      }

      this.errorMessageService.handleError(error, this.messageService);

      throw error;
    }
  }

  private goToNotFound() {
    return this.router.navigateByUrl('/not-found');
  }

  private createForm() {
    if (this.bankAccount == null) return;

    this.formGroup = this.fb.group({
      name: [this.bankAccount.name, [Validators.required]],
      currency: [this.bankAccount.currency, [Validators.required]],
      enabled: [this.bankAccount.enabled, [Validators.required]],
    });

    this.formGroup
      .get('currency')!
      .valueChanges.pipe(untilDestroyed(this))
      .subscribe(currency => {
        this.currency = currency;
      });
  }
}
