import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { TdDialogService } from '@covalent/core/dialogs';
import { Group } from 'src/app/@core/models/group';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';
import { TitleService, GroupsService } from 'src/app/@core/services';

@Component({
  selector: 'app-group-single-page',
  templateUrl: './group-single-page.component.html',
  styleUrls: ['./group-single-page.component.scss'],
})
export class GroupSinglePageComponent implements OnInit, OnDestroy {
  public group: Group;
  public sharedLinkLoading = false;

  private activatedRouteSubscription: Subscription;
  private groupId: string;
  private updateGroupSubscription: Subscription;

  constructor(
    private groupsService: GroupsService,
    private activatedRoute: ActivatedRoute,
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private router: Router,
    private titleService: TitleService,
  ) {}

  ngOnInit(): void {
    this.activatedRouteSubscription = this.activatedRoute.params.subscribe(params => this.loadGroup(params.id));
  }

  ngOnDestroy(): void {
    if (this.activatedRouteSubscription) {
      this.activatedRouteSubscription.unsubscribe();
    }
    if (this.updateGroupSubscription) {
      this.updateGroupSubscription.unsubscribe();
    }
  }

  async createInvite(): Promise<void> {
    this.sharedLinkLoading = true;

    try {
      const sharedLink = await this.groupsService.generateShareLink(this.groupId);

      this.dialogService.openPrompt({
        title: this.translocoService.translate('link-created'),
        message: this.translocoService.translate('link-created-message'),
        value: `${window.location.origin}/finances/shared/invite/${sharedLink}`,
        cancelButton: this.translocoService.translate('cancel'),
        acceptButton: this.translocoService.translate('ok'),
      });
    } finally {
      this.sharedLinkLoading = false;
    }
  }

  async editGroup(): Promise<void> {
    /** TODO: Allow to edit other properties from group and i18n it */
    const newName = await this.dialogService
      .openPrompt({
        title: 'Editar grupo',
        message: '',
        value: this.group.name,
        cancelButton: 'Cancelar',
        acceptButton: 'Editar',
      })
      .afterClosed()
      .pipe(take(1))
      .toPromise();

    if (newName !== this.group.name) {
      await this.groupsService.editGroup({ ...this.group, name: newName });
    }
  }

  private async loadGroup(groupId: string): Promise<void> {
    this.groupId = groupId;

    if (this.updateGroupSubscription) {
      this.updateGroupSubscription.unsubscribe();
    }

    if (this.groupId) {
      this.updateGroupSubscription = this.groupsService.checkIfGroupChanged(this.groupId).subscribe(updatedGroup => {
        this.group = { ...this.group, name: updatedGroup.name };
      });
    }

    const group = await this.groupsService.getGroup(groupId);

    this.titleService.changeTitle('group-name', {
      name: group.name,
    });

    if (!group) {
      this.router.navigateByUrl('/404');
    }

    this.group = group;
  }
}
