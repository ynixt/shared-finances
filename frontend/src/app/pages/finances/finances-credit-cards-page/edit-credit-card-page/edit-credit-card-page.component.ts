import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { startWith } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { ProgressSpinner } from 'primeng/progressspinner';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { CurrencySelectorComponent } from '../../../../components/currency-selector/currency-selector.component';
import { CreditCardDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/creditCard';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { DEFAULT_ERROR_LIFE } from '../../../../util/error-util';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { CreditCardService } from '../../services/credit-card.service';

@Component({
  selector: 'app-edit-credit-card-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ProgressSpinner,
    ButtonDirective,
    CurrencySelectorComponent,
    FormsModule,
    InputText,
    InputNumber,
    ReactiveFormsModule,
    ToggleSwitch,
    ConfirmDialog,
  ],
  templateUrl: './edit-credit-card-page.component.html',
  styleUrl: './edit-credit-card-page.component.scss',
  providers: [ConfirmationService],
})
@UntilDestroy()
export class EditCreditCardPageComponent {
  card: CreditCardDto | null = null;
  loading = true;
  submitting = false;
  formGroup: FormGroup | undefined = undefined;
  currency: string = 'USD';

  constructor(
    private route: ActivatedRoute,
    private creditCardService: CreditCardService,
    private router: Router,
    private fb: FormBuilder,
    private messageService: MessageService,
    private translateService: TranslateService,
    private confirmationService: ConfirmationService,
    private errorMessageService: ErrorMessageService,
  ) {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');

      if (id) {
        this.getCard(id);
      } else {
        this.goToNotFound();
      }
    });
  }

  async submit() {
    if (this.formGroup == null || this.card == null || this.formGroup.invalid || this.submitting) {
      return;
    }

    this.submitting = true;

    try {
      await this.creditCardService.editCreditCard(this.card.id, {
        newName: this.formGroup.value.name,
        newCurrency: this.formGroup.value.currency,
        newEnabled: this.formGroup.value.enabled,
        newTotalLimit: this.formGroup.value.totalLimit,
        newDueDay: this.formGroup.value.dueDay,
        newDaysBetweenDueAndClosing: this.formGroup.value.daysBetweenDueAndClosing,
        newDueOnNextBusinessDay: this.formGroup.value.dueOnNextBusinessDay,
      });

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.creditCardsPage.editCreditCardPage.successMessage'),
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
        this.deleteCard();
      },
    });
  }

  private async deleteCard() {
    if (this.card == null || this.submitting) return;

    this.submitting = true;

    try {
      await this.creditCardService.deleteCreditCard(this.card.id);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.creditCardsPage.editCreditCardPage.successDeleteMessage'),
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

  private async getCard(id: string): Promise<void> {
    this.loading = true;

    try {
      this.card = await this.creditCardService.getCreditCard(id);
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
    if (this.card == null) return;

    this.formGroup = this.fb.group({
      name: [this.card.name, [Validators.required]],
      currency: [this.card.currency, [Validators.required]],
      enabled: [this.card.enabled, [Validators.required]],
      totalLimit: [this.card.totalLimit, [Validators.required, Validators.min(0)]],
      dueDay: [this.card.dueDay, [Validators.required, Validators.min(1), Validators.max(31)]],
      daysBetweenDueAndClosing: [this.card.daysBetweenDueAndClosing, [Validators.required, Validators.min(0), Validators.max(31)]],
      dueOnNextBusinessDay: [this.card.dueOnNextBusinessDay, [Validators.required]],
    });

    this.formGroup
      .get('currency')!
      .valueChanges.pipe(untilDestroyed(this), startWith(this.card.currency))
      .subscribe(currency => {
        this.currency = currency;
      });
  }
}
