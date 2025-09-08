import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { faPeopleGroup } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { ProgressSpinner } from 'primeng/progressspinner';

import { GroupDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { FinancesTitleBarComponent, FinancesTitleBarExtraButton } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupUserListComponent } from '../../components/group-user-list/group-user-list.component';
import { GroupService } from '../../services/group.service';

@Component({
  selector: 'app-overview-group-page',
  imports: [ProgressSpinner, FinancesTitleBarComponent, TranslatePipe, GroupUserListComponent],
  templateUrl: './overview-group-page.component.html',
  styleUrl: './overview-group-page.component.scss',
})
@UntilDestroy()
export class OverviewGroupPageComponent {
  group: GroupDto | null = null;
  loading = true;
  submitting = false;

  extraButtons: FinancesTitleBarExtraButton[] = [
    {
      routerLink: 'team',
      rounded: true,
      tooltip: 'financesPage.groupsPage.overviewPage.manageTeam',
      icon: faPeopleGroup,
    },
  ];

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private groupService: GroupService,
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

  askForConfirmationToDelete() {}

  private async getGroup(id: string): Promise<void> {
    this.loading = true;

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
}
