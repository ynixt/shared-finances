import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';

import { UserService } from '../../../../services/user.service';
import { DEFAULT_ERROR_LIFE } from '../../../../util/error-util';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { BankAccountService } from '../../services/bank-account.service';

@Component({
  selector: 'app-new-bank-account-page',
  imports: [FinancesTitleBarComponent, TranslatePipe, ReactiveFormsModule, ButtonDirective, InputText, InputNumber],
  templateUrl: './new-bank-account-page.component.html',
  styleUrl: './new-bank-account-page.component.scss',
})
export class NewBankAccountPageComponent {
  readonly formGroup: FormGroup;

  submitting = false;

  constructor(
    fb: FormBuilder,
    public userService: UserService,
    private bankAccountService: BankAccountService,
    private messageService: MessageService,
    private translateService: TranslateService,
    private router: Router,
  ) {
    this.formGroup = fb.group({
      name: ['', [Validators.required]],
      balance: [undefined, []],
    });
  }

  async submit() {
    if (this.formGroup.invalid || this.submitting) {
      return;
    }

    this.submitting = true;

    try {
      await this.bankAccountService.createBankAccount(this.formGroup.value);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.bankAccountsPage.newBankAccountPage.successMessage'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await this.router.navigate(['..']);
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
