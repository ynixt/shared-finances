import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Toast } from 'primeng/toast';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { TurnstileWidgetComponent } from '../../components/turnstile-widget/turnstile-widget.component';
import { AuthHttpService } from '../../services/auth-http.service';
import { ErrorMessageService } from '../../services/error-message.service';

@Component({
  selector: 'app-confirm-email-page',
  imports: [NavbarComponent, TranslatePipe, Toast, TurnstileWidgetComponent, Button],
  templateUrl: './confirm-email-page.component.html',
  providers: [MessageService],
})
export class ConfirmEmailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authHttp = inject(AuthHttpService);
  private readonly messageService = inject(MessageService);
  private readonly errorMessageService = inject(ErrorMessageService);

  /** noToken: missing query param; ready: user can submit; submitting; ok; error */
  status: 'noToken' | 'ready' | 'submitting' | 'ok' | 'error' = 'ready';
  token = '';
  turnstileToken: string | null = null;

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (this.token === '') {
      this.status = 'noToken';
    }
  }

  async confirm(): Promise<void> {
    if (!this.token || this.status === 'submitting') return;
    this.status = 'submitting';
    try {
      await this.authHttp.confirmEmail({
        token: this.token,
        turnstileToken: this.turnstileToken ?? undefined,
      });
      this.status = 'ok';
    } catch (err) {
      this.status = 'error';
      this.errorMessageService.handleError(err, this.messageService);
    }
  }

  goToLogin(): void {
    void this.router.navigateByUrl('/login');
  }
}
