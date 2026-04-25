import { Component, OnInit } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ProgressSpinner } from 'primeng/progressspinner';
import { Toast } from 'primeng/toast';

import { ErrorMessageService } from '../../services/error-message.service';
import { UserService } from '../../services/user.service';
import { UserActionEventService } from '../finances/services/user-action-event.service';
import { OnboardingService } from './onboarding.service';

@Component({
  selector: 'app-onboarding-page',
  imports: [TranslatePipe, ReactiveFormsModule, Toast, ProgressSpinner],
  templateUrl: './onboarding-page.component.html',
  styleUrl: './onboarding-page.component.scss',
  providers: [MessageService],
})
@UntilDestroy()
export class OnboardingPageComponent implements OnInit {
  submitting = false;
  onboardingHasDone = false;

  constructor(
    private userService: UserService,
    private router: Router,
    private messageService: MessageService,
    private onboardingService: OnboardingService,
    private userActionEventService: UserActionEventService,
    private errorMessageService: ErrorMessageService,
  ) {
    this.userService.getUser().then(u => {
      if (u && u.onboardingDone) {
        this.router.navigate(['/app']);
      }
    });

    this.userActionEventService.onboardingCompleted$.pipe(untilDestroyed(this)).subscribe(() => {
      this.onboardingDone();
    });
  }

  async ngOnInit() {
    try {
      const onboardingSuccess = await this.onboardingService.onboarding();

      if (!onboardingSuccess) return;

      this.onboardingDone();
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);

      throw error;
    }
  }

  private onboardingDone() {
    console.log('onboardingHasDone ' + this.onboardingHasDone);
    if (this.onboardingHasDone) return;

    this.onboardingHasDone = true;

    const currentUser = this.userService.user()!!;

    this.userService.changeUser({
      ...currentUser,
      onboardingDone: true,
    });

    setTimeout(() => {
      void this.router.navigate(['/app']);
    }, 50);
  }
}
