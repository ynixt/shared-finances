import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';

import { Group } from 'src/app/@core/models/group';
import { GroupsService, ErrorService } from 'src/app/@core/services';

@Component({
  selector: 'app-new-group',
  templateUrl: './new-group.component.html',
  styleUrls: ['./new-group.component.scss'],
})
export class NewGroupComponent implements OnInit {
  constructor(
    private router: Router,
    private groupsService: GroupsService,
    private errorService: ErrorService,
    private toast: HotToastService,
    private translocoService: TranslocoService,
  ) {}

  ngOnInit(): void {}

  async newGroup(groupInput: Partial<Group>): Promise<void> {
    await this.groupsService
      .newGroup(groupInput)
      .pipe(
        take(1),
        this.toast.observe({
          loading: this.translocoService.translate('creating'),
          success: this.translocoService.translate('creating-successful', { name: groupInput.name }),
          error: error =>
            this.errorService.getInstantErrorMessage(error, 'creating-error', 'creating-error-with-description', {
              name: groupInput.name,
            }),
        }),
      )
      .toPromise();

    this.router.navigateByUrl('/finances/shared');
  }
}
