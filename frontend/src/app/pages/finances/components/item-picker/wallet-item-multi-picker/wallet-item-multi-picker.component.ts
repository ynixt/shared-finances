import { Component, ViewChild, forwardRef, input } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { Chip } from 'primeng/chip';

import { PagedMultiSelectComponent } from '../../../../../components/paged-multi-select/paged-multi-select.component';
import { SimpleControlValueAccessor } from '../../../../../components/simple-control-value-accessor';
import { WalletItemSearchResponseDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { WalletItemOptionComponent } from '../wallet-item-option/wallet-item-option.component';

@Component({
  selector: 'app-wallet-item-multi-picker',
  standalone: true,
  imports: [FormsModule, Chip, PagedMultiSelectComponent, WalletItemOptionComponent],
  templateUrl: './wallet-item-multi-picker.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WalletItemMultiPickerComponent),
      multi: true,
    },
  ],
})
export class WalletItemMultiPickerComponent extends SimpleControlValueAccessor<string[]> {
  @ViewChild('pagedMultiSelect') private pagedMultiSelect?: PagedMultiSelectComponent;

  readonly optionsGetter = input.required<(page: number, query?: string) => Promise<WalletItemSearchResponseDto[]>>();
  readonly placeholder = input<string>();
  readonly pageSize = input(10);
  readonly showToggleAll = input(false);

  resetComponent(): void {
    this.pagedMultiSelect?.resetComponent();
  }

  override valueEquals(valueA: string[] | undefined, valueB: string[] | undefined): boolean {
    if (valueA === valueB) return true;
    if (valueA == null || valueB == null || valueA.length !== valueB.length) return false;

    return valueA.every((value, index) => value === valueB[index]);
  }
}
