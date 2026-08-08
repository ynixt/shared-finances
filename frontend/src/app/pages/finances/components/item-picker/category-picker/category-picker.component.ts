import { AfterViewInit, Component, ViewChild, computed, effect, forwardRef, inject, input } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { Select } from 'primeng/select';

import { PagedSelectComponent } from '../../../../../components/paged-select/paged-select.component';
import { SimpleControlValueAccessor } from '../../../../../components/simple-control-value-accessor';
import { CategoryDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { PageRequest } from '../../../../../models/pagination';
import { GroupCategoriesService } from '../../../services/group-categories.service';
import { GetAllCategoriesParams, UserCategoriesService } from '../../../services/user-categories.service';

@Component({
  selector: 'app-category-picker',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, PagedSelectComponent, Select],
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
export class CategoryPickerComponent extends SimpleControlValueAccessor<CategoryDto> implements AfterViewInit {
  private readonly userCategoriesService = inject(UserCategoriesService);
  private readonly groupCategoriesService = inject(GroupCategoriesService);

  @ViewChild('pagedSelect') pagedSelect: PagedSelectComponent | undefined = undefined;

  categories = input<readonly CategoryDto[] | undefined>(undefined);
  loadAllCategories = input<boolean>(false);
  optionsGetter = input<(page: number, query?: string | undefined) => Promise<CategoryDto[]>>(this.loadCategories.bind(this));
  groupId = input<string | undefined>(undefined);
  getAllCategoriesParams = input<GetAllCategoriesParams>({});
  pageSize = input<number>(10);
  sort = input<string | undefined>('name');
  placeholder = input<string>();
  showClear = input(true);
  appendTo = input<any>(undefined);
  filterCategoriesAfterLoad = input<(categories: CategoryDto[]) => CategoryDto[]>(categories => categories);
  readonly allCategoriesGetter = this.loadAllCategoriesForPicker.bind(this);
  readonly providedCategoryOptions = computed(() => this.filterCategoriesAfterLoad()([...(this.categories() ?? [])]));

  constructor() {
    super();

    effect(() => {
      this.groupId();
      this.categories();
      const loadAllCategories = this.loadAllCategories();

      const pagedSelect = this.pagedSelect;
      pagedSelect?.resetComponent();
      if (loadAllCategories && pagedSelect != null) {
        void this.preloadAllCategories(pagedSelect);
      }
    });
  }

  ngAfterViewInit(): void {
    if (this.loadAllCategories() && this.pagedSelect != null) {
      void this.preloadAllCategories(this.pagedSelect);
    }
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

  allCategoriesPageSize(): number {
    return Math.max(this.categories()?.length ?? 500, 1);
  }

  private async loadAllCategoriesForPicker(page = 0): Promise<CategoryDto[]> {
    if (page > 0) return [];

    const providedCategories = this.categories();
    if (providedCategories != null) {
      return this.filterCategoriesAfterLoad()([...providedCategories]);
    }

    const categories: CategoryDto[] = [];
    const pageSize = 500;
    let currentPage = 0;

    while (true) {
      const pageResult = await (this.groupId() == null
        ? this.userCategoriesService.getAllCategories(this.getAllCategoriesParams(), {
            size: pageSize,
            sort: this.sort(),
            page: currentPage,
          })
        : this.groupCategoriesService.getAllCategories(this.groupId()!, this.getAllCategoriesParams(), {
            size: pageSize,
            sort: this.sort(),
            page: currentPage,
          }));

      categories.push(...pageResult.content);
      if (pageResult.last === true || pageResult.content.length < pageSize) break;
      currentPage += 1;
    }

    return this.filterCategoriesAfterLoad()(categories);
  }

  private async preloadAllCategories(pagedSelect: PagedSelectComponent): Promise<void> {
    await pagedSelect.onLazyLoad({ first: 0, last: 0 });
  }
}
