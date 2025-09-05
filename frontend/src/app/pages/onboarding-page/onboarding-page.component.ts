import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { Toast } from 'primeng/toast';

import { CurrencySelectorComponent } from '../../components/currency-selector/currency-selector.component';
import { LangButtonComponent } from '../../components/lang-button/lang-button.component';
import { UserService } from '../../services/user.service';
import { DEFAULT_ERROR_LIFE } from '../../util/error-util';
import { DEFAULT_SUCCESS_LIFE } from '../../util/success-util';

@Component({
  selector: 'app-onboarding-page',
  imports: [TranslatePipe, ReactiveFormsModule, CurrencySelectorComponent, ButtonDirective, LangButtonComponent, Toast],
  templateUrl: './onboarding-page.component.html',
  styleUrl: './onboarding-page.component.scss',
  providers: [MessageService],
})
export class OnboardingPageComponent {
  form: FormGroup;
  submitting = false;

  constructor(
    private translateService: TranslateService,
    private userService: UserService,
    private router: Router,
    private messageService: MessageService,
    formBuilder: FormBuilder,
  ) {
    this.userService.getUser().then(u => {
      if (u && u.defaultCurrency != null) {
        this.router.navigate(['/app']);
      }
    });

    this.form = formBuilder.group({
      currency: ['', [Validators.required]],
    });
  }

  async submit() {
    if (this.form.invalid || this.submitting) return;

    this.submitting = true;

    try {
      await this.userService.changeDefaultCurrency(this.form.value.currency);

      await this.router.navigate(['/app']);
    } catch (err) {
      this.submitting = false;

      console.error(err);

      this.messageService.add({
        severity: 'error',
        summary: this.translateService.instant('error.genericTitle'),
        detail: this.translateService.instant('error.genericMessage'),
        life: DEFAULT_ERROR_LIFE,
      });
    }
  }
}
