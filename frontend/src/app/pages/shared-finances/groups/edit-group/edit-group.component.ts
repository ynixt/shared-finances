import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';
import { Group } from 'src/app/@core/models/group';
import { ErrorService, GroupsService } from 'src/app/@core/services';

@Component({
  selector: 'app-edit-group',
  templateUrl: './edit-group.component.html',
  styleUrls: ['./edit-group.component.scss'],
})
export class EditGroupComponent implements OnInit {
  group: Group;

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private groupsService: GroupsService,
    private errorService: ErrorService,
    private toast: HotToastService,
    private translocoService: TranslocoService,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.params.subscribe(params => this.getGroup(params.groupId));
  }

  async editGroup(groupInput: Group): Promise<void> {
    await this.groupsService
      .editGroup(groupInput)
      .pipe(
        take(1),
        this.toast.observe({
          loading: this.translocoService.translate('editing'),
          success: this.translocoService.translate('editing-successful', { name: groupInput.name }),
          error: error =>
            this.errorService.getInstantErrorMessage(error, 'editing-error', 'editing-error-with-description', {
              name: groupInput.name,
            }),
        }),
      )
      .toPromise();

    this.router.navigateByUrl('/finances/shared');
  }

  private async getGroup(groupId: string): Promise<void> {
    try {
      const group = await this.groupsService.getGroup(groupId);

      if (!group) {
        this.router.navigateByUrl('/404');
      }

      this.group = group;
    } catch (err) {
      this.toast.error(this.errorService.getInstantErrorMessage(err, 'generic-error', 'generic-error-with-description'));
    }
  }
}
