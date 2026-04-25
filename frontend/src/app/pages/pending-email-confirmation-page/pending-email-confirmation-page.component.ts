import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputOtp } from 'primeng/inputotp';
import { InputText } from 'primeng/inputtext';
import { Toast } from 'primeng/toast';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { TurnstileWidgetComponent } from '../../components/turnstile-widget/turnstile-widget.component';
import { AuthHttpService } from '../../services/auth-http.service';
import { ErrorMessageService } from '../../services/error-message.service';

@Component({
  selector: 'app-pending-email-confirmation-page',
  imports: [NavbarComponent, TranslatePipe, ReactiveFormsModule, InputText, InputOtp, Button, Toast, TurnstileWidgetComponent],
  templateUrl: './pending-email-confirmation-page.component.html',
  providers: [MessageService],
})
export class PendingEmailConfirmationPageComponent implements OnInit {
  readonly otpLength = 8;

  @ViewChild('turnstile') private turnstileWidget?: TurnstileWidgetComponent;
  @ViewChild('otpInput') private otpInput?: InputOtp;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authHttp = inject(AuthHttpService);
  private readonly messageService = inject(MessageService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly translateService = inject(TranslateService);

  email = '';
  changeForm!: FormGroup;
  confirmForm!: FormGroup;
  /** Shared Turnstile token for confirm/resend/change-email (reset after each successful request). */
  turnstileToken: string | null = null;
  cooldownSeconds = 0;
  confirming = false;
  private cooldownTimer?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') ?? '';
    this.confirmForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9]{8}$/)]],
    });
    this.changeForm = this.fb.group({
      newEmail: ['', [Validators.required, Validators.email]],
    });
  }

  async confirmByCode(): Promise<void> {
    if (this.confirmForm.invalid || this.confirming || !this.turnstileToken) return;

    const return_to = this.route.snapshot.queryParamMap.get('return_to');
    const code = String(this.confirmForm.value.code ?? '')
      .trim()
      .toUpperCase();

    if (!/^[A-Z0-9]{8}$/.test(code)) return;

    this.confirming = true;

    try {
      await this.authHttp.confirmEmail({
        token: code,
        turnstileToken: this.turnstileToken ?? undefined,
      });

      this.resetTurnstile();

      if (return_to) {
        await this.router.navigate(['/login'], { queryParams: { return_to } });
      } else {
        await this.router.navigateByUrl('/login');
      }
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    } finally {
      this.confirming = false;
    }
  }

  async resend(): Promise<void> {
    try {
      if (!this.turnstileToken) return;

      const ack = await this.authHttp.resendConfirmationEmail({
        email: this.email,
        turnstileToken: this.turnstileToken,
      });

      this.startCooldown(ack.cooldownSeconds);
      this.resetTurnstile();

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('authFlows.pending.resent'),
      });
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    }
  }

  async changeEmail(): Promise<void> {
    if (this.changeForm.invalid || !this.turnstileToken) return;

    try {
      const ack = await this.authHttp.changePendingEmail({
        currentEmail: this.email,
        newEmail: this.changeForm.value.newEmail,
        turnstileToken: this.turnstileToken ?? undefined,
      });

      this.email = this.changeForm.value.newEmail;

      this.startCooldown(ack.cooldownSeconds);
      this.resetTurnstile();

      this.messageService.add({ severity: 'success', summary: 'OK', detail: 'Email updated' });
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    }
  }

  handleOtpPaste(event: ClipboardEvent) {
    event.preventDefault();

    const pasted = event.clipboardData?.getData('text') ?? '';

    if (!pasted) return;

    const sanitized = pasted.replace(/[\s\-]+/g, '').slice(0, 8);

    this.changeOtpCode(sanitized);
  }

  private changeOtpCode(sanitized: string) {
    this.confirmForm.patchValue({
      code: sanitized,
    });

    if (this.otpInput) {
      const inputs: HTMLInputElement[] = this.otpInput.$el.querySelectorAll('input');

      let i = 0;
      inputs?.forEach(input => {
        const letter: string | undefined = sanitized[i++];
        input.value = letter ?? '';
      });
    }
  }

  protected handleInputOtp(event: Event) {
    event.preventDefault();

    const pasted = (event.target as HTMLInputElement).value ?? '';

    if (!pasted) return;

    const sanitized = this.sanitizeOtpInput(pasted);

    this.changeOtpCode(sanitized);
  }

  private sanitizeOtpInput(input: string): string {
    return input.replace(/[\s\-]+/g, '').slice(0, 8);
  }

  protected otpInputKeydown(event: KeyboardEvent, index: any, events: any, inputElement: HTMLInputElement, confirmCodeButton: Button) {
    console.log(event);
    if (!/^[a-zA-Z]$/.test(event.key) || event.ctrlKey || event.altKey || event.metaKey) {
      events.keydown(event);
      return;
    }

    event.preventDefault();

    const current: string = (this.confirmForm.get('code')?.value ?? '').padStart(this.otpLength, ' ');
    const sanitized = this.sanitizeOtpInput(event.key);
    const newCode = (
      current.substring(0, index - 1) +
      sanitized +
      current.substring(index - 1 + sanitized.length, current.length)
    ).substring(0, this.otpLength);

    this.changeOtpCode(newCode);

    let next: Element | null | undefined = inputElement.nextElementSibling;

    if (index == this.otpLength / 2) {
      next = next?.nextElementSibling?.nextElementSibling;
    }

    if (next instanceof HTMLInputElement) {
      next.focus();
    }

    if (index == this.otpLength) {
      setTimeout(() => {
        confirmCodeButton.el.nativeElement.focus();
      }, 100);
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

  private resetTurnstile(): void {
    this.turnstileToken = null;
    this.turnstileWidget?.reset();
  }
}
