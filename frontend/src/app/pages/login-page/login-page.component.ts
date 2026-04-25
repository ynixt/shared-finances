import { Component, OnInit, effect, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { filter } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { InputOtp } from 'primeng/inputotp';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { Toast } from 'primeng/toast';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { TurnstileWidgetComponent } from '../../components/turnstile-widget/turnstile-widget.component';
import { LoginResultDto } from '../../models/generated/com/ynixt/sharedfinances/application/web/dto/auth';
import { AuthService } from '../../services/auth.service';
import { ErrorMessageService } from '../../services/error-message.service';
import { OpenAuthPreferencesService } from '../../services/open-auth-preferences.service';
import { TokenSyncService } from '../../services/token-sync.service';
import { UserService } from '../../services/user.service';
import { DEFAULT_ERROR_LIFE } from '../../util/error-util';

@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    Toast,
    TranslatePipe,
    RouterLink,
    NavbarComponent,
    ButtonDirective,
    Password,
    InputText,
    InputOtp,
    TurnstileWidgetComponent,
  ],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
  providers: [MessageService],
})
@UntilDestroy()
export class LoginPageComponent implements OnInit {
  protected readonly openAuthPreferences = inject(OpenAuthPreferencesService);

  loginForm!: FormGroup;
  mfaForm!: FormGroup;

  loading = false;
  loginResponse: LoginResultDto | undefined;
  loginTurnstileToken: string | null = null;
  mfaTurnstileToken: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private translateService: TranslateService,
    private messageService: MessageService,
    private userService: UserService,
    private tokenSyncService: TokenSyncService,
    private errorMessageService: ErrorMessageService,
  ) {
    effect(() => {
      const error = userService.error();

      if (error != null) {
        console.error(error);

        this.messageService.add({
          severity: 'error',
          summary: this.translateService.instant('loginPage.error.title'),
          detail: this.translateService.instant('error.genericMessage'),
          life: DEFAULT_ERROR_LIFE,
        });
      }
    });
  }

  async ngOnInit(): Promise<void> {
    this.router.events
      .pipe(
        filter(e => e instanceof NavigationEnd),
        untilDestroyed(this),
      )
      .subscribe((e: NavigationEnd) => {
        if (e.urlAfterRedirects === '/login') {
          this.initLoginForm();
        }
      });

    await this.initLoginForm();
  }

  private async initLoginForm() {
    this.loading = false;
    this.loginResponse = undefined;

    try {
      await this.getFlow();
    } catch (error) {
      await this.getFlow();
    }
  }

  async submit(): Promise<void> {
    if (this.loginForm.invalid || !this.loginTurnstileToken) {
      return;
    }

    this.loading = true;

    try {
      this.loginResponse = await this.authService.submitLogin({
        email: this.loginForm.value.identifier,
        password: this.loginForm.value.password,
        turnstileToken: this.loginTurnstileToken ?? undefined,
      });

      if (this.loginResponse.mfaRequired) {
        this.loading = false;
      } else {
        await this.authService.waitForLoginSuccess();
      }
    } catch (err) {
      this.loading = false;
      this.errorMessageService.handleError(err, this.messageService);
      throw err;
    }
  }

  async submitMfa(): Promise<void> {
    if (this.mfaForm.invalid || this.loginResponse?.mfaRequired != true || !this.loginResponse?.mfaChallengeId || !this.mfaTurnstileToken) {
      return;
    }

    try {
      this.loading = true;

      await this.authService.submitMfa({
        code: this.mfaForm.value.code,
        challengeId: this.loginResponse.mfaChallengeId,
        turnstileToken: this.mfaTurnstileToken ?? undefined,
      });

      await this.authService.waitForLoginSuccess();

      this.loginResponse = undefined;
    } catch (err) {
      this.loading = false;
      this.errorMessageService.handleError(err, this.messageService);
      throw err;
    }
  }

  private async getFlow() {
    if (this.loginForm == null) {
      this.loginForm = this.fb.group({
        identifier: ['', [Validators.required]],
        password: ['', [Validators.required]],
        method: ['password'],
      });
    }

    if (this.mfaForm == null) {
      this.mfaForm = this.fb.group({
        code: ['', [Validators.required]],
      });
    }
  }
}
