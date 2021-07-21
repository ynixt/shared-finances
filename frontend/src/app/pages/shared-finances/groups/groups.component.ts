import { Component, OnInit } from '@angular/core';
import { TdDialogService } from '@covalent/core/dialogs';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';

import { Group } from 'src/app/@core/models/group';
import { ErrorService } from 'src/app/@core/services';
import { GroupsService } from 'src/app/@core/services/groups.service';

@Component({
  selector: 'app-groups',
  templateUrl: './groups.component.html',
  styleUrls: ['./groups.component.scss'],
})
export class GroupsComponent implements OnInit {
  groups$: Promise<Group[]>;

  constructor(
    private groupsService: GroupsService,
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
  ) {}

  ngOnInit(): void {
    this.groups$ = this.groupsService.getGroups();
  }

  async delete(group: Group): Promise<void> {
    const confirm = await this.dialogService
      .openConfirm({
        title: this.translocoService.translate('confirm'),
        message: this.translocoService.translate('delete-confirm', { name: group.name }),
        cancelButton: this.translocoService.translate('cancel'),
        acceptButton: this.translocoService.translate('delete'),
        width: '500px',
      })
      .afterClosed()
      .pipe(take(1))
      .toPromise();
    if (confirm) {
      this.toast.observe;
      await this.groupsService
        .deleteGroup(group.id)
        .pipe(
          take(1),
          this.toast.observe({
            loading: this.translocoService.translate('deleting'),
            success: this.translocoService.translate('deleting-successful', { name: group.name }),
            error: error =>
              this.errorService.getInstantErrorMessage(error, 'deleting-error', 'deleting-error-with-description', {
                name: group.name,
              }),
          }),
        )
        .toPromise();
    }
  }
}
