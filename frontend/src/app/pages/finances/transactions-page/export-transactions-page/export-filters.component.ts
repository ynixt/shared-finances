import { Component, ViewChild, effect, inject, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { MultiSelect } from 'primeng/multiselect';
import { Select } from 'primeng/select';

import { ChipEditorComponent } from '../../../../components/chip-editor/chip-editor.component';
import { PagedMultiSelectComponent } from '../../../../components/paged-multi-select/paged-multi-select.component';
import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import {
  ExportFormat__Options,
  WalletEntryType,
  WalletEntryType__Options,
} from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { WalletItemMultiPickerComponent } from '../../components/item-picker/wallet-item-multi-picker/wallet-item-multi-picker.component';
import { AdvancedDatePickerComponent } from '../../components/wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';

@Component({
  selector: 'app-export-filters',
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    Select,
    MultiSelect,
    Checkbox,
    ButtonDirective,
    ChipEditorComponent,
    PagedMultiSelectComponent,
    WalletItemMultiPickerComponent,
    AdvancedDatePickerComponent,
  ],
  templateUrl: './export-filters.component.html',
})
export class ExportFiltersComponent {
  private readonly translate = inject(TranslateService);

  @ViewChild('walletItemsSelect') private walletItemsSelect?: WalletItemMultiPickerComponent;
  @ViewChild('categoriesSelect') private categoriesSelect?: PagedMultiSelectComponent;

  readonly form = input.required<FormGroup>();
  readonly groups = input.required<GroupWithRoleDto[]>();
  readonly walletItemsGetter = input.required<(page: number, query?: string) => Promise<WalletItemSearchResponseDto[]>>();
  readonly categoriesGetter = input.required<(page: number, query?: string) => Promise<CategoryDto[]>>();
  readonly scopeKey = input<string | null>(null);
  readonly disabled = input(false);
  readonly submitting = input(false);
  readonly groupChanged = output<string | null>();
  readonly submitted = output<void>();

  readonly formats = ExportFormat__Options;
  readonly entryTypes: Array<{ label: string; value: WalletEntryType }> = WalletEntryType__Options.map(value => ({
    label: this.translate.instant(`enums.walletEntryType.${value}`),
    value,
  }));
  readonly confirmedOptions = [
    { labelKey: 'confirmed.yes', value: true },
    { labelKey: 'confirmed.no', value: false },
  ];

  constructor() {
    effect(() => {
      this.scopeKey();
      queueMicrotask(() => {
        this.walletItemsSelect?.resetComponent();
        this.categoriesSelect?.resetComponent();
      });
    });
  }
}
