import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Toast } from 'primeng/toast';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { TurnstileWidgetComponent } from '../../components/turnstile-widget/turnstile-widget.component';
import { AuthHttpService } from '../../services/auth-http.service';
import { ErrorMessageService } from '../../services/error-message.service';

@Component({
  selector: 'app-pending-email-confirmation-page',
  imports: [NavbarComponent, TranslatePipe, ReactiveFormsModule, InputText, Button, Toast, TurnstileWidgetComponent],
  templateUrl: './pending-email-confirmation-page.component.html',
  providers: [MessageService],
})
export class PendingEmailConfirmationPageComponent implements OnInit {
  @ViewChild('turnstile') private turnstileWidget?: TurnstileWidgetComponent;

  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly authHttp = inject(AuthHttpService);
  private readonly messageService = inject(MessageService);
  private readonly errorMessageService = inject(ErrorMessageService);

  email = '';
  changeForm!: FormGroup;
  /** Shared Turnstile token for resend and change-email (reset after each successful request). */
  turnstileToken: string | null = null;
  cooldownSeconds = 0;
  private cooldownTimer?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') ?? '';
    this.changeForm = this.fb.group({
      newEmail: ['', [Validators.required, Validators.email]],
    });
  }

  async resend(): Promise<void> {
    try {
      const ack = await this.authHttp.resendConfirmationEmail({
        email: this.email,
        turnstileToken: this.turnstileToken ?? undefined,
      });
      this.startCooldown(ack.cooldownSeconds);
      this.turnstileWidget?.reset();
      this.messageService.add({ severity: 'success', summary: 'OK', detail: 'Email sent' });
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    }
  }

  async changeEmail(): Promise<void> {
    if (this.changeForm.invalid) return;
    try {
      const ack = await this.authHttp.changePendingEmail({
        currentEmail: this.email,
        newEmail: this.changeForm.value.newEmail,
        turnstileToken: this.turnstileToken ?? undefined,
      });
      this.email = this.changeForm.value.newEmail;
      this.startCooldown(ack.cooldownSeconds);
      this.turnstileWidget?.reset();
      this.messageService.add({ severity: 'success', summary: 'OK', detail: 'Email updated' });
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    }
  }

  private startCooldown(seconds: number): void {
    this.cooldownSeconds = seconds;
    clearInterval(this.cooldownTimer);
    this.cooldownTimer = setInterval(() => {
      this.cooldownSeconds--;
      if (this.cooldownSeconds <= 0) {
        clearInterval(this.cooldownTimer);
      }
    }, 1000);
  }
}
