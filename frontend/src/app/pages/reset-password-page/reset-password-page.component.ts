import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Password } from 'primeng/password';
import { Toast } from 'primeng/toast';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { TurnstileWidgetComponent } from '../../components/turnstile-widget/turnstile-widget.component';
import { AuthHttpService } from '../../services/auth-http.service';
import { ErrorMessageService } from '../../services/error-message.service';
import { confirmPasswordValidator } from '../registration-page/confirm-password.validator';
import { passwordValidator } from '../registration-page/password-validator';

@Component({
  selector: 'app-reset-password-page',
  imports: [NavbarComponent, TranslatePipe, ReactiveFormsModule, Password, Button, Toast, RouterLink, TurnstileWidgetComponent],
  templateUrl: './reset-password-page.component.html',
  providers: [MessageService],
})
export class ResetPasswordPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authHttp = inject(AuthHttpService);
  private readonly messageService = inject(MessageService);
  private readonly errorMessageService = inject(ErrorMessageService);

  form!: FormGroup;
  token = '';
  turnstileToken: string | null = null;

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    this.form = this.fb.group(
      {
        password: ['', [Validators.required, passwordValidator]],
        confirmPassword: ['', [Validators.required]],
      },
      { validators: confirmPasswordValidator },
    );
  }

  async submit(): Promise<void> {
    if (this.form.invalid || !this.token) return;
    try {
      await this.authHttp.resetPassword({
        token: this.token,
        newPassword: this.form.value.password,
        turnstileToken: this.turnstileToken ?? undefined,
      });
      await this.router.navigateByUrl('/login');
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    }
  }
}
