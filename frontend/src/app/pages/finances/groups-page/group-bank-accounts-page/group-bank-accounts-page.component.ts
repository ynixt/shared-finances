import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faTrash } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ProgressSpinner } from 'primeng/progressspinner';
import { Ripple } from 'primeng/ripple';
import { TableModule } from 'primeng/table';

import { UserAvatarComponent } from '../../../../components/user-avatar/user-avatar.component';
import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { BankAccountForGroupAssociateDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/bankAccount';
import { GroupPermissions__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { DEFAULT_ERROR_LIFE } from '../../../../util/error-util';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupAssociationService } from '../../services/group-association.service';
import { GroupService } from '../../services/group.service';

@Component({
  selector: 'app-group-bank-accounts-page',
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
  templateUrl: './group-bank-accounts-page.component.html',
  styleUrl: './group-bank-accounts-page.component.scss',
  providers: [ConfirmationService],
})
@UntilDestroy()
export class GroupBankAccountsPageComponent {
  group: GroupWithRoleDto | null = null;
  loading = true;
  submitting = false;

  bankAccounts: BankAccountForGroupAssociateDto[] = [];

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

  async showDeleteConfirmation(bankAccount: BankAccountForGroupAssociateDto) {
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
        this.unassociateBankAccount(bankAccount);
      },
    });
  }

  private async unassociateBankAccount(bankAccount: BankAccountForGroupAssociateDto) {
    if (this.submitting || this.groupId == null) return;

    this.submitting = true;

    try {
      await this.groupAssociationService.unassociateBank(this.groupId, bankAccount.id);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.bankAccountsAssociatedPage.unassociateBankAccountSuccessMessage'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      this.submitting = false;

      this.getBankAccounts();
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
      await this.getBankAccounts();

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

  private async getBankAccounts() {
    if (this.group == null) return;

    this.loading = true;
    this.bankAccounts = await this.groupAssociationService.findAllAssociatedBanks(this.group.id);
    this.loading = false;
  }

  private goToNotFound() {
    return this.router.navigateByUrl('/not-found');
  }

  protected readonly GroupPermissions__Obj = GroupPermissions__Obj;
  protected readonly faTrash = faTrash;
}
