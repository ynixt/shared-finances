import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ColorPicker } from 'primeng/colorpicker';
import { InputText } from 'primeng/inputtext';

import { PagedSelectComponent } from '../../../../components/paged-select/paged-select.component';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { UserService } from '../../../../services/user.service';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { UserCategoriesService } from '../../services/user-categories.service';

@Component({
  selector: 'app-new-user-category-page',
  imports: [ButtonDirective, FinancesTitleBarComponent, InputText, ReactiveFormsModule, TranslatePipe, ColorPicker, PagedSelectComponent],
  templateUrl: './new-user-category-page.component.html',
  styleUrl: './new-user-category-page.component.scss',
})
@UntilDestroy()
export class NewUserCategoryPageComponent {
  readonly formGroup: FormGroup;

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

  async loadCategoriesForParentPicker(page = 0, query: string | undefined): Promise<CategoryDto[]> {
    return (
      await this.categoriesService.getAllCategories(
        {
          onlyRoot: true,
          mountChildren: false,
          query,
        },
        {
          size: 10,
          sort: 'name',
          page,
        },
      )
    ).content;
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
