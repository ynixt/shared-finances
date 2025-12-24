import { Component, OnInit, effect } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NavigationEnd, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { filter } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { Toast } from 'primeng/toast';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { AuthService } from '../../services/auth.service';
import { ErrorMessageService } from '../../services/error-message.service';
import { TokenSyncService } from '../../services/token-sync.service';
import { UserService } from '../../services/user.service';
import { DEFAULT_ERROR_LIFE } from '../../util/error-util';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, Toast, TranslatePipe, NavbarComponent, ButtonDirective, Password, InputText],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
  providers: [MessageService],
})
@UntilDestroy()
export class LoginPageComponent implements OnInit {
  form!: FormGroup;
  loading = false;

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

    try {
      await this.getFlow();
    } catch (error) {
      await this.getFlow();
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      return;
    }

    this.loading = true;

    try {
      await this.authService.submitLogin({
        email: this.form.value.identifier,
        passwordHash: this.form.value.password,
      });

      await this.authService.waitForLoginSuccess();
    } catch (err) {
      this.loading = false;
      this.errorMessageService.handleError(err, this.messageService);
      throw err;
    }
  }

  private async getFlow() {
    if (this.form == null) {
      this.form = this.fb.group({
        identifier: ['', [Validators.required]],
        password: ['', [Validators.required]],
        method: ['password'],
      });
    }
  }
}
