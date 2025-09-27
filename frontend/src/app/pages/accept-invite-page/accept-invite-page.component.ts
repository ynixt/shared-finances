import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ProgressSpinner } from 'primeng/progressspinner';
import { Ripple } from 'primeng/ripple';
import { Toast } from 'primeng/toast';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { AppResponseErrorDto } from '../../models/generated/com/ynixt/sharedfinances/application/web/dto';
import { GroupInfoForInviteDto } from '../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups/invite';
import { LocalDatePipe } from '../../pipes/local-date.pipe';
import { ErrorMessageService } from '../../services/error-message.service';
import { UserService } from '../../services/user.service';
import { DEFAULT_ERROR_LIFE } from '../../util/error-util';
import { GroupInvitationService } from '../finances/services/group-invitation.service';

@Component({
  selector: 'app-accept-invite-page',
  imports: [NavbarComponent, TranslatePipe, ProgressSpinner, ButtonDirective, Ripple, LocalDatePipe, Toast],
  templateUrl: './accept-invite-page.component.html',
  styleUrl: './accept-invite-page.component.scss',
  providers: [MessageService],
})
@UntilDestroy()
export class AcceptInvitePageComponent {
  loading = true;
  inviteInfo: GroupInfoForInviteDto | undefined = undefined;
  acceptingInvite = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private groupInvitationService: GroupInvitationService,
    private userService: UserService,
    private translateService: TranslateService,
    private messageService: MessageService,
    private errorMessageService: ErrorMessageService,
  ) {
    this.route.paramMap.pipe(untilDestroyed(this)).subscribe(params => {
      const id = params.get('id');

      if (id) {
        this.getInviteInfo(id);
      } else {
        this.goToNotFound();
      }
    });
  }

  async acceptInvite() {
    if (this.inviteInfo == null) return;

    this.acceptingInvite = true;

    const user = await this.userService.getUser();

    if (user == null) {
      await this.router.navigate(['register'], {
        queryParams: {
          return_to: this.router.url,
        },
      });
    } else {
      try {
        const { id } = await this.groupInvitationService.acceptInvite(this.inviteInfo.id);
        await this.router.navigate(['app', 'groups', id]);
      } catch (error) {
        console.error(error);
        this.acceptingInvite = false;

        this.errorMessageService.handleError(error, this.messageService);
      }
    }
  }

  declineInvite() {
    this.router.navigate(['/app']);
  }

  private async getInviteInfo(id: string): Promise<void> {
    this.loading = true;

    try {
      this.inviteInfo = await this.groupInvitationService.getInviteInfo(id);
      this.loading = false;
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 404 || error.status === 400) {
          await this.goToNotFound();
          return;
        }
      }

      throw error;
    }
  }

  private goToNotFound() {
    return this.router.navigateByUrl('/not-found');
  }
}
