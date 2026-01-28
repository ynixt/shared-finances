import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ProgressSpinner } from 'primeng/progressspinner';
import { Toast } from 'primeng/toast';

import { CurrencySelectorComponent } from '../../components/currency-selector/currency-selector.component';
import { LangButtonComponent } from '../../components/lang-button/lang-button.component';
import { RequiredFieldAsteriskComponent } from '../../components/required-field-asterisk/required-field-asterisk.component';
import { ErrorMessageService } from '../../services/error-message.service';
import { UserService } from '../../services/user.service';
import { OnboardingService } from './onboarding.service';

@Component({
  selector: 'app-onboarding-page',
  imports: [TranslatePipe, ReactiveFormsModule, LangButtonComponent, Toast, ProgressSpinner],
  templateUrl: './onboarding-page.component.html',
  styleUrl: './onboarding-page.component.scss',
  providers: [MessageService],
})
export class OnboardingPageComponent {
  submitting = false;

  constructor(
    private userService: UserService,
    private router: Router,
    private messageService: MessageService,
    private onboardingService: OnboardingService,
    private errorMessageService: ErrorMessageService,
  ) {
    this.userService.getUser().then(u => {
      if (u && u.onboardingDone) {
        this.router.navigate(['/app']);
      }
    });

    setTimeout(async () => {
      try {
        await this.onboardingService.onboarding();

        const currentUser = this.userService.user()!!;
        this.userService.changeUser({
          ...currentUser,
          onboardingDone: true,
        });

        this.router.navigate(['/app']);
      } catch (error) {
        this.errorMessageService.handleError(error, this.messageService);

        throw error;
      }
    }, 3000);
  }
}
