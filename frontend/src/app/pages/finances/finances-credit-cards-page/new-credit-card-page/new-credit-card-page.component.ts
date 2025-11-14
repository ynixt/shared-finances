import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { CurrencySelectorComponent } from '../../../../components/currency-selector/currency-selector.component';
import { RequiredFieldAsteriskComponent } from '../../../../components/required-field-asterisk/required-field-asterisk.component';
import { UserService } from '../../../../services/user.service';
import { DEFAULT_ERROR_LIFE } from '../../../../util/error-util';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { CreditCardService } from '../../services/credit-card.service';

@Component({
  selector: 'app-new-credit-card-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ReactiveFormsModule,
    ButtonDirective,
    InputText,
    InputNumber,
    ToggleSwitch,
    CurrencySelectorComponent,
    RequiredFieldAsteriskComponent,
  ],
  templateUrl: './new-credit-card-page.component.html',
  styleUrl: './new-credit-card-page.component.scss',
})
@UntilDestroy()
export class NewCreditCardPageComponent {
  readonly formGroup: FormGroup;
  submitting = false;
  currency: string = 'USD';

  constructor(
    fb: FormBuilder,
    private creditCardService: CreditCardService,
    private messageService: MessageService,
    private translateService: TranslateService,
    private router: Router,
    private route: ActivatedRoute,
    private userService: UserService,
  ) {
    this.formGroup = fb.group({
      name: ['', [Validators.required]],
      currency: ['', [Validators.required]],
      totalLimit: [0, [Validators.required, Validators.min(0)]],
      dueDay: [1, [Validators.required, Validators.min(1), Validators.max(31)]],
      daysBetweenDueAndClosing: [10, [Validators.required, Validators.min(0), Validators.max(31)]],
      dueOnNextBusinessDay: [true, [Validators.required]],
    });

    this.userService.getUser().then(u => {
      if (u) {
        this.formGroup.get('currency')!!.setValue(u.defaultCurrency);
      }
    });

    this.formGroup
      .get('currency')!
      .valueChanges.pipe(untilDestroyed(this))
      .subscribe(currency => {
        this.currency = currency;
      });
  }

  async submit() {
    if (this.formGroup.invalid || this.submitting) {
      return;
    }

    this.submitting = true;

    try {
      await this.creditCardService.createCreditCard(this.formGroup.value);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.creditCardsPage.newCreditCardPage.successMessage'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await this.router.navigate(['..'], { relativeTo: this.route });
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
}
