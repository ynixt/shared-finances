import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { faPencil, faPeopleGroup } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { filter } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ProgressSpinner } from 'primeng/progressspinner';

import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { GroupPermissions__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { FinancesTitleBarComponent, FinancesTitleBarExtraButton } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupUserListComponent } from '../../components/group-user-list/group-user-list.component';
import { GroupService } from '../../services/group.service';
import { GroupsActionEventService } from '../../services/groups-action-event.service';

@Component({
  selector: 'app-overview-group-page',
  imports: [ProgressSpinner, FinancesTitleBarComponent, TranslatePipe, GroupUserListComponent],
  templateUrl: './overview-group-page.component.html',
  styleUrl: './overview-group-page.component.scss',
})
@UntilDestroy()
export class OverviewGroupPageComponent {
  group: GroupWithRoleDto | null = null;
  loading = true;
  submitting = false;

  extraButtons: FinancesTitleBarExtraButton[] = this.createExtraButtons();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private groupService: GroupService,
    private groupsActionEventService: GroupsActionEventService,
    private messageService: MessageService,
    private errorMessageService: ErrorMessageService,
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

  private async getGroup(id: string): Promise<void> {
    this.loading = true;

    try {
      this.group = await this.groupService.getGroup(id);
      this.trackGroup(id);

      this.extraButtons = this.createExtraButtons();

      this.loading = false;
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 404 || error.status === 400) {
          await this.goToNotFound();
          return;
        }
      }

      this.errorMessageService.handleError(error, this.messageService);

      throw error;
    }
  }

  private goToNotFound() {
    return this.router.navigateByUrl('/not-found');
  }

  private createExtraButtons(): FinancesTitleBarExtraButton[] {
    const extraButtons: FinancesTitleBarExtraButton[] = [];

    extraButtons.push({
      routerLink: 'team',
      rounded: true,
      tooltip: 'financesPage.groupsPage.overviewPage.manageTeam',
      icon: faPeopleGroup,
    });

    if (this.group?.permissions?.includes(GroupPermissions__Obj.EDIT_GROUP)) {
      extraButtons.push({
        routerLink: 'edit',
        rounded: true,
        tooltip: 'general.edit',
        icon: faPencil,
      });
    }

    return extraButtons;
  }

  private trackGroup(groupId: string) {
    this.groupsActionEventService.groupUpdated$
      .pipe(
        untilDestroyed(this),
        filter(e => e.groupId == groupId),
      )
      .subscribe(e => this.groupUpdated(e.data));

    this.groupsActionEventService.groupDeleted$
      .pipe(
        untilDestroyed(this),
        filter(e => e.groupId == groupId),
      )
      .subscribe(e => this.groupDeleted(e.data));
  }

  private groupUpdated(newGroup: GroupWithRoleDto) {
    this.group = newGroup;
  }

  private groupDeleted(_: string) {
    this.router.navigate(['/app']);
  }
}
