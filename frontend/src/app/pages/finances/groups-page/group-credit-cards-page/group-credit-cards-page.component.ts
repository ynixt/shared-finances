import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faTrash } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { filter } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ProgressSpinner } from 'primeng/progressspinner';
import { Ripple } from 'primeng/ripple';
import { TableModule } from 'primeng/table';

import { UserAvatarComponent } from '../../../../components/user-avatar/user-avatar.component';
import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { CreditCardForGroupAssociateDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/creditCard';
import { GroupPermissions__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { DEFAULT_ERROR_LIFE } from '../../../../util/error-util';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupAssociationService } from '../../services/group-association.service';
import { GroupService } from '../../services/group.service';
import { GroupActionEvent, GroupsActionEventService } from '../../services/groups-action-event.service';

@Component({
  selector: 'app-group-credit-cards-accounts-page',
  imports: [
    FinancesTitleBarComponent,
    ProgressSpinner,
    TranslatePipe,
    ButtonDirective,
    FaIconComponent,
    Ripple,
    TableModule,
    UserAvatarComponent,
    ConfirmDialog,
  ],
  templateUrl: './group-credit-cards-page.component.html',
  styleUrl: './group-credit-cards-page.component.scss',
  providers: [ConfirmationService],
})
@UntilDestroy()
export class GroupCreditCardsPageComponent {
  group: GroupWithRoleDto | null = null;
  loading = true;
  submitting = false;

  creditCards: CreditCardForGroupAssociateDto[] = [];

  private groupId: string | undefined;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private groupService: GroupService,
    private groupAssociationService: GroupAssociationService,
    private confirmationService: ConfirmationService,
    private translateService: TranslateService,
    private messageService: MessageService,
    private errorMessageService: ErrorMessageService,
    private groupsActionEventService: GroupsActionEventService,
  ) {
    this.route.paramMap.pipe(untilDestroyed(this)).subscribe(params => {
      const id = params.get('id');

      if (id) {
        this.getGroup(id);
      } else {
        this.goToNotFound();
      }
    });

    this.groupsActionEventService.creditCardAssociated$
      .pipe(
        untilDestroyed(this),
        filter(e => this.group != null && e.groupId == this.groupId),
      )
      .subscribe(async e => this.newCreditCardWasAssociated(e));

    this.groupsActionEventService.creditCardUnassociated$
      .pipe(
        untilDestroyed(this),
        filter(e => this.group != null && e.groupId == this.groupId),
      )
      .subscribe(async e => this.creditCardWasUnassociated(e));
  }

  async showDeleteConfirmation(creditCard: CreditCardForGroupAssociateDto) {
    if (!this.canRemoveCreditCard) {
      return;
    }

    this.confirmationService.confirm({
      message: this.translateService.instant('general.genericConfirmation'),
      header: this.translateService.instant('general.confirmation'),
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('general.unassociate'),
      rejectLabel: this.translateService.instant('general.cancel'),
      acceptButtonProps: {
        severity: 'danger',
      },
      rejectButtonProps: {
        severity: 'secondary',
      },
      accept: () => {
        this.unassociateCreditCard(creditCard);
      },
    });
  }

  private async unassociateCreditCard(creditCard: CreditCardForGroupAssociateDto) {
    if (!this.canRemoveCreditCard || this.submitting || this.groupId == null) return;

    this.submitting = true;

    try {
      await this.groupAssociationService.unassociateCreditCard(this.groupId, creditCard.id);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.creditCardsAssociatedPage.unassociateCreditCardSuccessMessage'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      this.submitting = false;
    } catch (err) {
      this.submitting = false;

      this.messageService.add({
        severity: 'error',
        summary: this.translateService.instant('error.genericTitle'),
        detail: this.translateService.instant('error.genericMessage'),
        life: DEFAULT_ERROR_LIFE,
      });

      console.error(err);
    }
  }

  private async getGroup(id: string): Promise<void> {
    this.loading = true;
    this.groupId = id;

    try {
      this.group = await this.groupService.getGroup(id);
      await this.getCreditCards();

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

  private async getCreditCards() {
    if (this.group == null) return;

    this.loading = true;
    this.creditCards = await this.groupAssociationService.findAllAssociatedCreditCards(this.group.id);
    this.loading = false;
  }

  private goToNotFound() {
    return this.router.navigateByUrl('/not-found');
  }

  private async newCreditCardWasAssociated(_: GroupActionEvent<string>) {
    // TODO improve this
    this.creditCards = await this.groupAssociationService.findAllAssociatedCreditCards(this.group!!.id!!);
  }

  private async creditCardWasUnassociated(e: GroupActionEvent<string>) {
    const index = this.creditCards.findIndex(b => b.id === e.data);

    if (index != -1) {
      const newItems = [...this.creditCards];
      newItems.splice(index, 1);

      this.creditCards = newItems;
    }
  }

  get canAddCreditCard(): boolean {
    return this.group?.permissions.includes(GroupPermissions__Obj.ADD_CREDIT_CARD) === true;
  }

  get canRemoveCreditCard(): boolean {
    return this.group?.permissions.includes(GroupPermissions__Obj.REMOVE_CREDIT_CARD) === true;
  }

  protected readonly faTrash = faTrash;
}
