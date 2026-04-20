import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ColorPicker } from 'primeng/colorpicker';
import { InputText } from 'primeng/inputtext';

import { ErrorMessageService } from '../../../../services/error-message.service';
import { UserService } from '../../../../services/user.service';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { CategoryPickerComponent } from '../../components/item-picker/category-picker/category-picker.component';
import { ConceptPickerComponent } from '../../components/item-picker/concept-picker/concept-picker.component';
import { isCustomCategoryConceptOption, resolveCategoryConceptPayload } from '../../services/category-concept-form.util';
import { GroupCategoriesService } from '../../services/group-categories.service';
import { GetAllCategoriesParams } from '../../services/user-categories.service';

@Component({
  selector: 'app-new-group-category-page',
  imports: [
    ButtonDirective,
    FinancesTitleBarComponent,
    InputText,
    ReactiveFormsModule,
    TranslatePipe,
    ColorPicker,
    CategoryPickerComponent,
    ConceptPickerComponent,
  ],
  templateUrl: './new-group-category-page.component.html',
  styleUrl: './new-group-category-page.component.scss',
})
@UntilDestroy()
export class NewGroupCategoryPageComponent {
  readonly formGroup: FormGroup;
  readonly getAllCategoriesParams: GetAllCategoriesParams = {
    onlyRoot: true,
    mountChildren: false,
  };

  submitting = false;
  groupId: string | undefined;

  constructor(
    fb: FormBuilder,
    public userService: UserService,
    private messageService: MessageService,
    private router: Router,
    private route: ActivatedRoute,
    private groupCategoriesService: GroupCategoriesService,
    private errorMessageService: ErrorMessageService,
  ) {
    this.formGroup = fb.group({
      name: ['', [Validators.required]],
      parent: [undefined, []],
      color: ['#000000', [Validators.required]],
      conceptId: [undefined, [Validators.required]],
      customConceptName: ['', []],
    });

    this.formGroup
      .get('conceptId')
      ?.valueChanges.pipe(untilDestroyed(this))
      .subscribe(value => this.updateCustomConceptValidators(value));

    this.groupId = this.route.snapshot.paramMap.get('id') ?? undefined;
  }

  async submit() {
    if (this.formGroup.invalid || this.submitting || !this.groupId) {
      return;
    }

    this.submitting = true;

    try {
      const conceptPayload = resolveCategoryConceptPayload(this.formGroup.value.conceptId, this.formGroup.value.customConceptName);

      await this.groupCategoriesService.newCategory(this.groupId, {
        name: this.formGroup.value.name,
        color: this.formGroup.value.color,
        parentId: this.formGroup.value.parent?.id,
        conceptId: conceptPayload.conceptId,
        customConceptName: conceptPayload.customConceptName,
      });
      await this.router.navigate(['..'], { relativeTo: this.route });
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
      this.submitting = false;
      throw error;
    }
  }

  get customConceptSelected(): boolean {
    return isCustomCategoryConceptOption(this.formGroup.value.conceptId);
  }

  private updateCustomConceptValidators(conceptId: string | null | undefined): void {
    const customConceptControl = this.formGroup.get('customConceptName');
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
