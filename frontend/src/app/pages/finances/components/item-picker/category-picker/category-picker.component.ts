import { Component, ViewChild, effect, forwardRef, inject, input } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { PagedSelectComponent } from '../../../../../components/paged-select/paged-select.component';
import { SimpleControlValueAccessor } from '../../../../../components/simple-control-value-accessor';
import { CategoryDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { PageRequest } from '../../../../../models/pagination';
import { GroupCategoriesService } from '../../../services/group-categories.service';
import { GetAllCategoriesParams, UserCategoriesService } from '../../../services/user-categories.service';

@Component({
  selector: 'app-category-picker',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, PagedSelectComponent],
  templateUrl: './category-picker.component.html',
  styleUrl: './category-picker.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CategoryPickerComponent),
      multi: true,
    },
  ],
})
export class CategoryPickerComponent extends SimpleControlValueAccessor<CategoryDto> {
  private readonly userCategoriesService = inject(UserCategoriesService);
  private readonly groupCategoriesService = inject(GroupCategoriesService);

  @ViewChild('pagedSelect') pagedSelect: PagedSelectComponent | undefined = undefined;

  optionsGetter = input<(page: number, query?: string | undefined) => Promise<CategoryDto[]>>(this.loadCategories.bind(this));
  groupId = input<string | undefined>(undefined);
  getAllCategoriesParams = input<GetAllCategoriesParams>({});
  pageSize = input<number>(10);
  sort = input<string | undefined>('name');
  filterCategoriesAfterLoad = input<(categories: CategoryDto[]) => CategoryDto[]>(categories => categories);

  constructor() {
    super();

    effect(() => {
      this.groupId();

      this.pagedSelect?.resetComponent();
    });
  }

  async loadCategories(page = 0, query: string | undefined): Promise<CategoryDto[]> {
    const groupId = this.groupId();

    const params: GetAllCategoriesParams = {
      ...this.getAllCategoriesParams(),
      query,
    };

    const pageRequest: PageRequest = {
      size: this.pageSize() + 1,
      sort: this.sort(),
      page,
    };

    const categories = (
      await (groupId == null
        ? this.userCategoriesService.getAllCategories(params, pageRequest)
        : this.groupCategoriesService.getAllCategories(groupId, params, pageRequest))
    ).content;

    return this.filterCategoriesAfterLoad()(categories);
  }
}
