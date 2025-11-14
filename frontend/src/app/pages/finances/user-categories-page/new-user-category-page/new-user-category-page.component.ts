import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ColorPicker } from 'primeng/colorpicker';
import { InputText } from 'primeng/inputtext';

import { RequiredFieldAsteriskComponent } from '../../../../components/required-field-asterisk/required-field-asterisk.component';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { UserService } from '../../../../services/user.service';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { CategoryPickerComponent } from '../../components/item-picker/category-picker/category-picker.component';
import { GetAllCategoriesParams, UserCategoriesService } from '../../services/user-categories.service';

@Component({
  selector: 'app-new-user-category-page',
  imports: [
    ButtonDirective,
    FinancesTitleBarComponent,
    InputText,
    ReactiveFormsModule,
    TranslatePipe,
    ColorPicker,
    CategoryPickerComponent,
    RequiredFieldAsteriskComponent,
  ],
  templateUrl: './new-user-category-page.component.html',
  styleUrl: './new-user-category-page.component.scss',
})
@UntilDestroy()
export class NewUserCategoryPageComponent {
  readonly formGroup: FormGroup;
  readonly getAllCategoriesParams: GetAllCategoriesParams = {
    onlyRoot: true,
    mountChildren: false,
  };

  submitting = false;

  constructor(
    fb: FormBuilder,
    public userService: UserService,
    private messageService: MessageService,
    private router: Router,
    private route: ActivatedRoute,
    private categoriesService: UserCategoriesService,
    private errorMessageService: ErrorMessageService,
  ) {
    this.formGroup = fb.group({
      name: ['', [Validators.required]],
      parent: [undefined, []],
      color: ['#000000', [Validators.required]],
    });
  }

  async submit() {
    if (this.formGroup.invalid || this.submitting) {
      return;
    }

    this.submitting = true;

    try {
      await this.categoriesService.newCategory({
        name: this.formGroup.value.name,
        color: this.formGroup.value.color,
        parentId: this.formGroup.value.parent?.id,
      });
      await this.router.navigate(['..'], { relativeTo: this.route });
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
      this.submitting = false;
      throw error;
    }
  }
}
