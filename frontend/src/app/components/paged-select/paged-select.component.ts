import { NgTemplateOutlet } from '@angular/common';
import { Component, ContentChild, TemplateRef, ViewChild, forwardRef } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Select, SelectChangeEvent } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';

import { PagedSelectControlValueAccessor } from '../paged-select-control-value-accessor';

@Component({
  selector: 'app-paged-select',
  imports: [ReactiveFormsModule, FormsModule, Select, Skeleton, InputText, IconField, InputIcon, NgTemplateOutlet],
  templateUrl: './paged-select.component.html',
  styleUrl: './paged-select.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PagedSelectComponent),
      multi: true,
    },
  ],
})
export class PagedSelectComponent extends PagedSelectControlValueAccessor<any> {
  @ContentChild('item', { read: TemplateRef }) externalItemTemplate?: TemplateRef<any>;
  @ContentChild('selectedItem', { read: TemplateRef }) externalSelectedItemTemplate?: TemplateRef<any>;

  @ViewChild('select') select: Select | undefined;

  clearSelection() {
    this.writeValue(null);
    this.onChange(null);
  }

  onSelectionChange(event: SelectChangeEvent) {
    this.writeValue(event.value);
  }

  async onShow() {
    await this.onOverlayShow();

    // This is bad, I know, but this was the only method that I discover to force items to be reloaded
    this.select?._filterValue.set('loading');
    setTimeout(() => {
      this.select?._filterValue.set('');
      if (this.select?.scroller) {
        this.select.scroller.autoSize = false;
      }
    }, 0);
  }

  protected override disableScrollerAutoSize() {
    if (this.select?.scroller) {
      this.select.scroller.autoSize = false;
    }
  }

  protected override scrollScrollerToTop() {
    this.select?.scroller?.scrollTo({ top: 0 });
  }

  protected override putValueOnListIfListNotContainsValue(value: any) {
    if (value == null) return;

    const optionValueField = this.optionValue();
    const dataKey = this.dataKey();

    if (optionValueField == null) {
      const optionsKeysSet = new Set(this.options.map(o => o[dataKey]));
      const key = value[dataKey];

      if (!optionsKeysSet.has(key)) {
        this.options = [value, ...this.options];
        optionsKeysSet.add(key);
      }

      return;
    }

    const optionsSet = new Set(this.options.map(option => this.getOptionComparisonValue(option)));

    if (!optionsSet.has(value)) {
      optionsSet.add(value);
      this.options = [this.createFallbackOptionFromValue(value), ...this.options];
    }
  }
}
