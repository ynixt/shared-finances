import { Component, OnInit } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ProgressSpinner } from 'primeng/progressspinner';
import { Toast } from 'primeng/toast';

import { ErrorMessageService } from '../../services/error-message.service';
import { UserService } from '../../services/user.service';
import { OnboardingService } from './onboarding.service';

@Component({
  selector: 'app-onboarding-page',
  imports: [TranslatePipe, ReactiveFormsModule, Toast, ProgressSpinner],
  templateUrl: './onboarding-page.component.html',
  styleUrl: './onboarding-page.component.scss',
  providers: [MessageService],
})
export class OnboardingPageComponent implements OnInit {
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
  }

  async ngOnInit() {
    try {
      await this.onboardingService.onboarding();

      const currentUser = this.userService.user()!!;
      this.userService.changeUser({
        ...currentUser,
        onboardingDone: true,
      });

      setTimeout(() => {
        this.router.navigate(['/app']);
      }, 3000);
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);

      throw error;
    }
  }
}
