import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { InputText } from 'primeng/inputtext';
import { ProgressSpinner } from 'primeng/progressspinner';

import { RequiredFieldAsteriskComponent } from '../../../../components/required-field-asterisk/required-field-asterisk.component';
import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { GroupPermissions__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { DEFAULT_ERROR_LIFE } from '../../../../util/error-util';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupService } from '../../services/group.service';

@Component({
  selector: 'app-edit-group-page',
  imports: [
    ButtonDirective,
    FinancesTitleBarComponent,
    FormsModule,
    InputText,
    ReactiveFormsModule,
    TranslatePipe,
    ProgressSpinner,
    ConfirmDialog,
    RequiredFieldAsteriskComponent,
  ],
  templateUrl: './edit-group-page.component.html',
  styleUrl: './edit-group-page.component.scss',
  providers: [ConfirmationService],
})
@UntilDestroy()
export class EditGroupPageComponent {
  protected readonly GroupPermissions__Obj = GroupPermissions__Obj;

  formGroup: FormGroup | undefined;
  group: GroupWithRoleDto | null = null;
  loading = true;
  submitting = false;

  constructor(
    private fb: FormBuilder,
    private messageService: MessageService,
    private translateService: TranslateService,
    private groupService: GroupService,
    private router: Router,
    private route: ActivatedRoute,
    private confirmationService: ConfirmationService,
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

  async askForConfirmationToDelete() {
    this.confirmationService.confirm({
      message: this.translateService.instant('general.genericConfirmation'),
      header: this.translateService.instant('general.confirmation'),
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('general.delete'),
      rejectLabel: this.translateService.instant('general.cancel'),
      acceptButtonProps: {
        severity: 'danger',
      },
      rejectButtonProps: {
        severity: 'secondary',
      },
      accept: () => {
        this.deleteGroup();
      },
    });
  }

  async submit() {
    if (!this.formGroup || this.formGroup.invalid || this.submitting || !this.group) {
      return;
    }

    this.submitting = true;

    try {
      const group = await this.groupService.editGroup(this.group.id, this.formGroup.value);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.editGroupPage.successMessage'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await this.router.navigate(['..'], { relativeTo: this.route });
    } catch (err) {
      this.submitting = false;

      console.error(err);

      this.messageService.add({
        severity: 'error',
        summary: this.translateService.instant('error.genericTitle'),
        detail: this.translateService.instant('error.genericMessage'),
        life: DEFAULT_ERROR_LIFE,
      });
    }
  }

  private async getGroup(id: string) {
    this.loading = true;

    try {
      this.group = await this.groupService.getGroup(id);

      if (!this.group.permissions.includes(GroupPermissions__Obj.EDIT_GROUP)) {
        await this.goToNotFound();
        return;
      }

      this.createForm();
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

  private createForm() {
    if (this.group == null) return;

    this.formGroup = this.fb.group({
      name: [this.group.name, [Validators.required]],
    });
  }

  private async deleteGroup() {
    if (this.group == null || this.submitting) return;

    this.submitting = true;

    try {
      await this.groupService.deleteGroup(this.group.id);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.editGroupPage.successDeleteMessage'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await this.router.navigate(['/app']);
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
}
