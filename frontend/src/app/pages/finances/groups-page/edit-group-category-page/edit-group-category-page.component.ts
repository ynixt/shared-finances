import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ColorPicker } from 'primeng/colorpicker';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { InputText } from 'primeng/inputtext';
import { ProgressSpinner } from 'primeng/progressspinner';

import { RequiredFieldAsteriskComponent } from '../../../../components/required-field-asterisk/required-field-asterisk.component';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { UserService } from '../../../../services/user.service';
import { DEFAULT_ERROR_LIFE } from '../../../../util/error-util';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { CategoryPickerComponent } from '../../components/item-picker/category-picker/category-picker.component';
import { ConceptPickerComponent } from '../../components/item-picker/concept-picker/concept-picker.component';
import { isCustomCategoryConceptOption, isDebtSfConcept, resolveCategoryConceptPayload } from '../../services/category-concept-form.util';
import { GroupCategoriesService } from '../../services/group-categories.service';
import { GetAllCategoriesParams } from '../../services/user-categories.service';

@Component({
  selector: 'app-edit-group-category-page',
  imports: [
    ButtonDirective,
    FinancesTitleBarComponent,
    InputText,
    ReactiveFormsModule,
    TranslatePipe,
    ColorPicker,
    ProgressSpinner,
    ConfirmDialog,
    CategoryPickerComponent,
    ConceptPickerComponent,
    RequiredFieldAsteriskComponent,
  ],
  templateUrl: './edit-group-category-page.component.html',
  styleUrl: './edit-group-category-page.component.scss',
  providers: [ConfirmationService],
})
@UntilDestroy()
export class EditGroupCategoryPageComponent {
  readonly getAllCategoriesParams: GetAllCategoriesParams = {
    onlyRoot: true,
    mountChildren: false,
  };

  formGroup: FormGroup | undefined;
  category: CategoryDto | null = null;
  isDebtSfCategory = false;
  loading: boolean = true;
  submitting = false;
  groupId: string | undefined;

  constructor(
    private fb: FormBuilder,
    public userService: UserService,
    private messageService: MessageService,
    private router: Router,
    private route: ActivatedRoute,
    private groupCategoriesService: GroupCategoriesService,
    private errorMessageService: ErrorMessageService,
    private confirmationService: ConfirmationService,
    private translateService: TranslateService,
  ) {
    this.route.paramMap.pipe(untilDestroyed(this)).subscribe(params => {
      const groupId = params.get('id');
      const categoryId = params.get('categoryId');

      if (groupId && categoryId) {
        this.groupId = groupId;
        this.getCategory(groupId, categoryId);
      } else {
        this.goToNotFound();
      }
    });
  }

  filterCategoriesForParentPicker(categories: CategoryDto[]): CategoryDto[] {
    return categories.filter(category => category.id !== this.category?.id);
  }

  async submit() {
    if (this.formGroup == null || this.category == null || this.formGroup.invalid || this.submitting || !this.groupId) {
      return;
    }

    this.submitting = true;

    try {
      const formValue = this.formGroup.getRawValue();
      const conceptPayload = this.isDebtSfCategory
        ? { conceptId: this.category.conceptId, customConceptName: null }
        : resolveCategoryConceptPayload(formValue.conceptId, formValue.customConceptName);

      await this.groupCategoriesService.editCategory(this.groupId, this.category.id, {
        name: formValue.name,
        color: formValue.color,
        parentId: formValue.parent?.id,
        conceptId: conceptPayload.conceptId,
        customConceptName: conceptPayload.customConceptName,
      });
      await this.router.navigate(['../..'], { relativeTo: this.route });
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
      this.submitting = false;
      throw error;
    }
  }

  async askForConfirmationToDelete() {
    if (this.isDebtSfCategory) {
      return;
    }

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
        this.deleteCategory();
      },
    });
  }

  private async getCategory(groupId: string, categoryId: string): Promise<void> {
    try {
      this.loading = true;
      this.category = await this.groupCategoriesService.getCategory(groupId, categoryId);
      await this.createForm(groupId);
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

  private async createForm(groupId: string) {
    const currentCategory = this.category;
    if (currentCategory == null) return;

    let parentCategory: CategoryDto | null = null;

    if (currentCategory.parentId != null) {
      parentCategory = await this.groupCategoriesService.getCategory(groupId, currentCategory.parentId, { mountChildren: false });
    }

    const concepts = await this.groupCategoriesService.getAvailableConcepts();
    const currentConcept = concepts.find(concept => concept.id === currentCategory.conceptId);
    this.isDebtSfCategory = isDebtSfConcept(currentConcept);

    this.formGroup = this.fb.group({
      name: [currentCategory.name, [Validators.required]],
      parent: [parentCategory, []],
      color: [currentCategory.color, [Validators.required]],
      conceptId: [currentCategory.conceptId, [Validators.required]],
      customConceptName: ['', []],
    });

    if (currentCategory.children && currentCategory.children.length > 0) {
      this.formGroup.get('parent')?.disable();
    }

    this.formGroup
      .get('conceptId')
      ?.valueChanges.pipe(untilDestroyed(this))
      .subscribe(value => this.updateCustomConceptValidators(value));

    if (this.isDebtSfCategory) {
      this.formGroup.get('conceptId')?.disable({ emitEvent: false });
      this.formGroup.get('customConceptName')?.disable({ emitEvent: false });
    } else {
      this.updateCustomConceptValidators(currentCategory.conceptId);
    }
  }

  private async deleteCategory() {
    if (this.category == null || this.submitting || !this.groupId) return;

    this.submitting = true;

    try {
      await this.groupCategoriesService.deleteCategory(this.groupId, this.category.id);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.categoriesPage.editCategoriesPage.successDeleteMessage'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await this.router.navigate(['../..'], { relativeTo: this.route });
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

  get customConceptSelected(): boolean {
    return !this.isDebtSfCategory && isCustomCategoryConceptOption(this.formGroup?.value?.conceptId);
  }

  private updateCustomConceptValidators(conceptId: string | null | undefined): void {
    const customConceptControl = this.formGroup?.get('customConceptName');
    if (customConceptControl == null) {
      return;
    }

    if (isCustomCategoryConceptOption(conceptId)) {
      customConceptControl.setValidators([Validators.required, Validators.maxLength(255)]);
    } else {
      customConceptControl.clearValidators();
      customConceptControl.setValue('', { emitEvent: false });
    }
    customConceptControl.updateValueAndValidity({ emitEvent: false });
  }
}
