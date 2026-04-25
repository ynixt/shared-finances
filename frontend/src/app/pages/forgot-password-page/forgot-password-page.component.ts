import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Toast } from 'primeng/toast';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { TurnstileWidgetComponent } from '../../components/turnstile-widget/turnstile-widget.component';
import { AuthHttpService } from '../../services/auth-http.service';
import { ErrorMessageService } from '../../services/error-message.service';
import { FinancesTitleBarComponent } from '../finances/components/finances-title-bar/finances-title-bar.component';

@Component({
  selector: 'app-forgot-password-page',
  imports: [
    NavbarComponent,
    TranslatePipe,
    ReactiveFormsModule,
    InputText,
    Button,
    Toast,
    TurnstileWidgetComponent,
    FinancesTitleBarComponent,
  ],
  templateUrl: './forgot-password-page.component.html',
  providers: [MessageService],
})
export class ForgotPasswordPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authHttp = inject(AuthHttpService);
  private readonly messageService = inject(MessageService);
  private readonly errorMessageService = inject(ErrorMessageService);

  form: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });
  turnstileToken: string | null = null;
  submitted = false;

  async submit(): Promise<void> {
    if (this.form.invalid || !this.turnstileToken) return;

    try {
      await this.authHttp.forgotPassword({
        email: this.form.value.email,
        turnstileToken: this.turnstileToken ?? undefined,
      });

      this.submitted = true;
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    }
  }
}
