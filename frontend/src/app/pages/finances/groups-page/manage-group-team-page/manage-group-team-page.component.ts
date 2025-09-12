import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { QRCodeComponent } from 'angularx-qrcode';
import { ButtonDirective } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { ProgressSpinner } from 'primeng/progressspinner';

import { GroupInviteDto, GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { GroupPermissions__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { LocalDatePipe } from '../../../../pipes/local-date.pipe';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupUserTableComponent } from '../../components/group-user-table/group-user-table.component';
import { GroupInvitationService } from '../../services/group-invitation.service';
import { GroupService } from '../../services/group.service';

@Component({
  selector: 'app-manage-group-team-page',
  imports: [
    FinancesTitleBarComponent,
    ProgressSpinner,
    TranslatePipe,
    GroupUserTableComponent,
    ButtonDirective,
    QRCodeComponent,
    InputText,
    LocalDatePipe,
  ],
  templateUrl: './manage-group-team-page.component.html',
  styleUrl: './manage-group-team-page.component.scss',
})
@UntilDestroy()
export class ManageGroupTeamPageComponent {
  group: GroupWithRoleDto | null = null;
  loading = true;
  submitting = false;
  generatingInvitation = false;

  invitation = signal<GroupInviteDto | undefined>(undefined);
  invitationUrl = computed<string | undefined>(() => {
    const invitation = this.invitation();

    if (invitation == null) return undefined;

    return `${window.location.origin}/invite/${invitation.id}`;
  });

  private groupId: string | undefined;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private groupService: GroupService,
    private groupInvitationService: GroupInvitationService,
  ) {
    this.route.paramMap.pipe(untilDestroyed(this)).subscribe(params => {
      const id = params.get('id');

      if (id) {
        this.getGroup(id);
      } else {
        this.goToNotFound();
      }
    });
  }

  async generateInvitation() {
    if (this.groupId == null) return;

    this.generatingInvitation = true;

    try {
      this.invitation.set(await this.groupInvitationService.generateNewInvitation(this.groupId));
    } catch (err) {
      console.error(err);
    } finally {
      this.generatingInvitation = false;
    }
  }

  copyLinkToClipboard() {
    const url = this.invitationUrl();

    if (url == null) return;

    return navigator.clipboard.writeText(url);
  }

  private async getGroup(id: string): Promise<void> {
    this.loading = true;
    this.groupId = id;

    try {
      this.group = await this.groupService.getGroup(id);

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

  protected readonly GroupPermissions__Obj = GroupPermissions__Obj;
}
