import { NgTemplateOutlet } from '@angular/common';
import { Component, ContentChild, TemplateRef, ViewChild, forwardRef } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { MultiSelect, MultiSelectChangeEvent } from 'primeng/multiselect';
import { Skeleton } from 'primeng/skeleton';

import { PagedSelectControlValueAccessor } from '../paged-select-control-value-accessor';

@Component({
  selector: 'app-paged-multi-select',
  imports: [ReactiveFormsModule, FormsModule, MultiSelect, Skeleton, InputText, IconField, InputIcon, NgTemplateOutlet],
  templateUrl: './paged-multi-select.component.html',
  styleUrl: './paged-multi-select.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PagedMultiSelectComponent),
      multi: true,
    },
  ],
})
export class PagedMultiSelectComponent extends PagedSelectControlValueAccessor<any[]> {
  @ContentChild('item', { read: TemplateRef }) externalItemTemplate?: TemplateRef<any>;
  @ContentChild('selectedItems', { read: TemplateRef }) externalSelectedItemsTemplate?: TemplateRef<any>;

  @ViewChild('select') select: MultiSelect | undefined;

  clearSelection() {
    this.writeValue([]);
    this.onChange([]);
  }

  onSelectionChange(event: MultiSelectChangeEvent) {
    this.writeValue(event.value ?? []);
  }

  async onShow() {
    await this.onOverlayShow();
  }

  protected override disableScrollerAutoSize() {
    if (this.select?.scroller) {
      this.select.scroller.autoSize = false;
    }
  }

  protected override scrollScrollerToTop() {
    this.select?.scroller?.scrollTo({ top: 0 });
  }

  protected override putValueOnListIfListNotContainsValue(value: any[] | undefined) {
    if (value == null || value.length === 0) return;

    const optionValueField = this.optionValue();

    if (optionValueField == null) {
      const optionsSet = new Set(this.options);
      const missingValues: any[] = [];

      for (const currentValue of value) {
        if (!optionsSet.has(currentValue)) {
          optionsSet.add(currentValue);
          missingValues.push(currentValue);
        }
      }

      if (missingValues.length > 0) {
        this.options = [...missingValues, ...this.options];
      }
      return;
    }

    const optionsSet = new Set(this.options.map(option => this.getOptionComparisonValue(option)));
    const missingValues: any[] = [];

    for (const currentValue of value) {
      if (!optionsSet.has(currentValue)) {
        optionsSet.add(currentValue);
        missingValues.push(this.createFallbackOptionFromValue(currentValue));
      }
    }

    if (missingValues.length > 0) {
      this.options = [...missingValues, ...this.options];
    }
  }
}
