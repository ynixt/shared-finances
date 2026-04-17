import { Directive, computed, input, signal } from '@angular/core';
import { FormControl } from '@angular/forms';

import { debounceTime, distinctUntilChanged } from 'rxjs';

import { ScrollerOptions } from 'primeng/api';

import { SimpleControlValueAccessor } from './simple-control-value-accessor';

type LazyLoadEvent = {
  first: number;
  last: number;
};

@Directive()
export abstract class PagedSelectControlValueAccessor<TValue> extends SimpleControlValueAccessor<TValue> {
  optionsGetter = input<(page: number, query?: string | undefined) => Promise<any[]>>();
  placeholder = input<string>();
  componentClass = input<string>();
  pageSize = input<number>(10);
  allowFilter = input<boolean>(true);
  filterInMemory = input<boolean>(false);
  showToggleAll = input<boolean>(false);
  optionLabel = input<string>('name');
  optionValue = input<string>();
  dataKey = input<string>('id');

  loading = signal<boolean>(true);
  searchControl = new FormControl('');

  currentPage = 0;
  lastItemRequested = -1;
  currentPageBeforeFilter = 0;
  lastItemRequestedBeforeFilter = -1;
  options: any[] = [];
  optionsBeforeFilter: any[] = [];

  selectLoaded = false;

  scrollerOptions = computed<ScrollerOptions>(() => ({
    showLoader: true,
    step: this.currentPage * this.pageSize() - 1,
    onLazyLoad: this.onLazyLoad.bind(this),
    loading: this.loading(),
    lazy: true,
  }));

  constructor() {
    super();

    this.searchControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(value => {
      void this.onFilterChange(value ?? '');
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

  override writeValue(obj: TValue | undefined): void {
    this.putValueOnListIfListNotContainsValue(obj);

    this.value = obj;
    this.onChange(obj);
  }

  protected async onOverlayShow() {
    if (!this.selectLoaded) {
      // We need to manual dispare on lazy load for the first time
      this.selectLoaded = false;

      await this.onLazyLoad({ first: 0, last: 0 });
    } else {
      this.disableScrollerAutoSize();
    }
  }

  async onLazyLoad(event: LazyLoadEvent) {
    if (event.last >= this.lastItemRequested) {
      const page = event.last == -1 ? 0 : Math.floor(event.last / this.pageSize());

      await this.loadItems(page);
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

    this.scrollScrollerToTop();
  }

  protected abstract disableScrollerAutoSize(): void;

  protected abstract scrollScrollerToTop(): void;

  protected abstract putValueOnListIfListNotContainsValue(value: TValue | undefined): void;

  protected getOptionComparisonValue(option: any): any {
    const optionValueField = this.optionValue();

    if (optionValueField != null && option != null && typeof option === 'object') {
      return option[optionValueField];
    }

    return option;
  }

  protected createFallbackOptionFromValue(value: any): any {
    const optionValueField = this.optionValue();
    if (optionValueField == null || (value != null && typeof value === 'object')) {
      return value;
    }

    return {
      [optionValueField]: value,
      [this.optionLabel()]: String(value),
    };
  }

  private async loadItems(page: number, query?: string | undefined): Promise<void> {
    const oldLastItemRequested = this.lastItemRequested;

    try {
      this.lastItemRequested = this.pageSize() * ++this.currentPage;

      const fn = this.optionsGetter();

      if (fn) {
        this.loading.set(true);
        const items = await fn(page, query);

        if (page <= 0) {
          this.options = [...items];
          this.putValueOnListIfListNotContainsValue(this.value);
        } else {
          this.options = [...this.options, ...items];
        }
        this.loading.set(false);
      }
    } catch (error) {
      this.lastItemRequested = oldLastItemRequested;
      this.selectLoaded = false;
      throw error;
    }
  }
}
