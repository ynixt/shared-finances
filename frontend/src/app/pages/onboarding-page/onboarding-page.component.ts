import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { Toast } from 'primeng/toast';

import { CurrencySelectorComponent } from '../../components/currency-selector/currency-selector.component';
import { LangButtonComponent } from '../../components/lang-button/lang-button.component';
import { RequiredFieldAsteriskComponent } from '../../components/required-field-asterisk/required-field-asterisk.component';
import { ErrorMessageService } from '../../services/error-message.service';
import { UserService } from '../../services/user.service';
import { DEFAULT_ERROR_LIFE } from '../../util/error-util';
import { OnboardingService } from './onboarding.service';

@Component({
  selector: 'app-onboarding-page',
  imports: [
    TranslatePipe,
    ReactiveFormsModule,
    CurrencySelectorComponent,
    ButtonDirective,
    LangButtonComponent,
    Toast,
    RequiredFieldAsteriskComponent,
  ],
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
    private route: ActivatedRoute,
    private onboardingService: OnboardingService,
    private errorMessageService: ErrorMessageService,
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
      await this.onboardingService.createDefaultCategories();

      await this.router.navigate(['/app']);

      const return_to = this.route.snapshot.queryParamMap.get('return_to');

      if (return_to) {
        await this.router.navigateByUrl(return_to);
      } else {
        await this.router.navigateByUrl('/app');
      }
    } catch (err) {
      this.submitting = false;

      this.errorMessageService.handleError(err, this.messageService);
    }
  }
}
