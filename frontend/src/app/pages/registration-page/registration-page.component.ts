import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { Toast } from 'primeng/toast';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { CurrencySelectorComponent } from '../../components/currency-selector/currency-selector.component';
import { LanguagePickerComponent } from '../../components/language-picker/language-picker.component';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { RequiredFieldAsteriskComponent } from '../../components/required-field-asterisk/required-field-asterisk.component';
import { TimeZoneSelectorComponent, allTimezones } from '../../components/timezone-selector/time-zone-selector.component';
import { TurnstileWidgetComponent } from '../../components/turnstile-widget/turnstile-widget.component';
import { AuthService } from '../../services/auth.service';
import { ErrorMessageService } from '../../services/error-message.service';
import { groupArrayBy } from '../../util/collection-util';
import { DEFAULT_SUCCESS_LIFE } from '../../util/success-util';
import { promiseTimeout } from '../../util/timeout-util';
import { confirmPasswordValidator } from './confirm-password.validator';
import { passwordValidator } from './password-validator';

@Component({
  selector: 'app-registration-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Button,
    Password,
    InputText,
    TranslatePipe,
    Toast,
    ToggleSwitch,
    LanguagePickerComponent,
    NavbarComponent,
    RequiredFieldAsteriskComponent,
    CurrencySelectorComponent,
    TimeZoneSelectorComponent,
    TurnstileWidgetComponent,
  ],
  templateUrl: './registration-page.component.html',
  styleUrl: './registration-page.component.scss',
  providers: [MessageService],
})
export class RegistrationPageComponent implements OnInit {
  form!: FormGroup;
  submitting = false;
  turnstileToken: string | null = null;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private translateService: TranslateService,
    private messageService: MessageService,
    private router: Router,
    private route: ActivatedRoute,
    private errorMessageService: ErrorMessageService,
  ) {}

  async ngOnInit(): Promise<void> {
    try {
      await this.getFlow();
    } catch (error) {
      await this.getFlow();
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.submitting) {
      return;
    }

    this.submitting = true;

    try {
      const registerResult = await this.auth.submitRegistration({
        email: this.form.value.email,
        password: this.form.value.password,
        firstName: this.form.value.name.first,
        lastName: this.form.value.name.last,
        lang: this.form.value.language,
        tmz: this.form.value.tmz,
        defaultCurrency: this.form.value.defaultCurrency,
        acceptTerms: this.form.value.acceptTerms,
        acceptPrivacy: this.form.value.acceptPrivacy,
        gravatarOptIn: this.form.value.gravatarOptIn,
        turnstileToken: this.turnstileToken ?? undefined,
      });

      if (registerResult.pendingEmailConfirmation) {
        const return_to = this.route.snapshot.queryParamMap.get('return_to');
        await this.router.navigate(['/pending-email-confirmation'], {
          queryParams: {
            email: registerResult.email,
            ...(return_to ? { return_to } : {}),
          },
        });
        return;
      }

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('registerPage.success.title'),
        detail: this.translateService.instant('registerPage.success.message'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await promiseTimeout(DEFAULT_SUCCESS_LIFE);
      const return_to = this.route.snapshot.queryParamMap.get('return_to');

      if (return_to) {
        await this.router.navigate(['/login'], { queryParams: { return_to } });
      } else {
        await this.router.navigateByUrl('/login');
      }
    } catch (error) {
      this.submitting = false;

      this.errorMessageService.handleError(error, this.messageService);

      throw error;
    }
  }

  private async getFlow() {
    this.form = this.fb.group(
      {
        email: ['', [Validators.required, Validators.email]],
        name: this.fb.group({
          first: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(20)]],
          last: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(20)]],
        }),
        password: ['', [Validators.required, passwordValidator]],
        confirmPassword: ['', [Validators.required]],
        language: [undefined, [Validators.required]],
        tmz: [this.getBrowserTmz(), [Validators.required]],
        defaultCurrency: [undefined, [Validators.required]],
        acceptTerms: [false, [Validators.requiredTrue]],
        acceptPrivacy: [false, [Validators.requiredTrue]],
        gravatarOptIn: [false],
      },
      { validators: confirmPasswordValidator },
    );
  }

  private getBrowserTmz(): string | undefined {
    const browserTmz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const allTimezonesByName = groupArrayBy(allTimezones, it => it.name);

    if (allTimezonesByName.has(browserTmz)) {
      return browserTmz;
    }

    return undefined;
  }
}
