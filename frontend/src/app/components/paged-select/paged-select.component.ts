import { NgTemplateOutlet } from '@angular/common';
import { Component, ContentChild, TemplateRef, ViewChild, computed, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, FormControl, FormsModule, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { debounceTime, distinctUntilChanged } from 'rxjs';

import { ScrollerOptions } from 'primeng/api';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Select, SelectChangeEvent, SelectLazyLoadEvent } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';

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
export class PagedSelectComponent implements ControlValueAccessor {
  optionsGetter = input<(page: number, query?: string | undefined) => Promise<any[]>>();
  placeholder = input<string>();
  componentClass = input<string>();
  pageSize = input<number>(10);
  allowFilter = input<boolean>(true);
  filterInMemory = input<boolean>(false);
  optionLabel = input<string>('name');
  dataKey = input<string>('id');

  @ContentChild('item', { read: TemplateRef }) externalItemTemplate?: TemplateRef<any>;
  @ContentChild('selectedItem', { read: TemplateRef }) externalSelectedItemTemplate?: TemplateRef<any>;

  loading = signal<boolean>(true);

  @ViewChild('select') select: Select | undefined;

  value: any;
  disabled = false;
  currentPage = 0;
  lastItemRequested = -1;
  currentPageBeforeFilter = 0;
  lastItemRequestedBeforeFilter = -1;
  options: any[] = [];
  optionsBeforeFilter: any[] = [];

  scrollerOptions = computed<ScrollerOptions>(() => ({
    showLoader: true,
    step: this.currentPage * this.pageSize() - 1,
    onLazyLoad: this.onLazyLoad.bind(this),
    loading: this.loading(),
    lazy: true,
  }));

  searchControl = new FormControl('');

  private onChange = (_: any) => {};
  private onTouched = () => {};

  constructor() {
    this.searchControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(value => {
      this.onFilterChange(value ?? '');
    });
  }

  resetComponent() {
    this.loading.set(true);
    this.selectLoaded = false;
    this.currentPage = 0;
    this.lastItemRequested = -1;
    this.currentPageBeforeFilter = 0;
    this.lastItemRequestedBeforeFilter = -1;
    this.options = [];
    this.optionsBeforeFilter = [];
  }

  writeValue(obj: any): void {
    this.putValueOnListIfListNotContainsValue(obj);

    this.value = obj;
    this.onChange(obj);
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  clearSelection() {
    this.writeValue(null);
    this.onChange(null);
  }

  onSelectionChange(event: SelectChangeEvent) {
    this.writeValue(event.value);
  }

  selectLoaded = false;

  private async loadItems(page: number, query?: string | undefined): Promise<void> {
    this.lastItemRequested = this.pageSize() * ++this.currentPage;

    const fn = this.optionsGetter();

    if (fn) {
      this.loading.set(true);
      const items = await fn(page, query);

      if (page == 0) {
        this.options = [...items];
        this.putValueOnListIfListNotContainsValue(this.value);
      } else {
        this.options = [...this.options, ...items];
      }
      this.loading.set(false);
    }
  }

  async onLazyLoad(event: SelectLazyLoadEvent) {
    if (event.last >= this.lastItemRequested) {
      await this.loadItems(Math.ceil(event.last / this.pageSize()));

      if (!this.selectLoaded) {
        // This is bad, I know, but this was the only method that I discover to force items to be reloaded
        this.select?._filterValue.set('loading');
        setTimeout(() => {
          this.select?._filterValue.set('');
          if (this.select?.scroller) {
            this.select.scroller.autoSize = false;
          }
          this.selectLoaded = true;
        }, 0);
      }
    }
  }

  async onFilterChange(query: string) {
    if (query.trim().length > 0) {
      if (this.lastItemRequestedBeforeFilter === -1) {
        this.lastItemRequestedBeforeFilter = this.lastItemRequested;
        this.currentPageBeforeFilter = this.currentPage;
        this.optionsBeforeFilter = this.options;
      }

      this.currentPage = 0;
      this.lastItemRequested = -1;

      await this.loadItems(0, query);
    } else {
      this.lastItemRequested = this.lastItemRequestedBeforeFilter;
      this.currentPage = this.currentPageBeforeFilter;
      this.options = this.optionsBeforeFilter;

      this.lastItemRequestedBeforeFilter = -1;
      this.currentPageBeforeFilter = -1;
      this.optionsBeforeFilter = [];
    }

    this.select?.scroller?.scrollTo({ top: 0 });
  }

  private putValueOnListIfListNotContainsValue(value: any) {
    if (value == null) return;

    const s = new Set(this.options);

    if (!s.has(value)) {
      console.log(value);
      this.options = [value, ...this.options];
    }
  }
}
