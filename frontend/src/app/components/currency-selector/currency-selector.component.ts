import { Component, OnInit, forwardRef, input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { ScrollerOptions } from 'primeng/api';
import { Select } from 'primeng/select';

import { CurrencyCatalogService, CurrencyItem } from './currency-catalog.service';

@Component({
  selector: 'app-currency-selector',
  templateUrl: './currency-selector.component.html',
  styleUrl: './currency-selector.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CurrencySelectorComponent),
      multi: true,
    },
  ],
  imports: [FormsModule, Select],
})
export class CurrencySelectorComponent implements ControlValueAccessor, OnInit {
  placeholder = input('');
  showClear = input(false);
  compact = input(false);
  assetsUrl = input('/public/currencies.json');
  appendTo = input<any>(undefined);

  currencies: CurrencyItem[] = [];
  value: string | null = null;

  scrollerOptions: ScrollerOptions = {
    showLoader: true,
    scrollWidth: '240px',
  };

  private onChange = (_: any) => {};
  private onTouched = () => {};
  disabled = false;

  constructor(private readonly currencyCatalog: CurrencyCatalogService) {}

  ngOnInit(): void {
    this.loadCurrencies();
  }

  private loadCurrencies() {
    this.currencyCatalog.getCurrencies(this.assetsUrl()).subscribe({
      next: currencies => {
        this.currencies = currencies;
      },
    });
  }

  onSelectCurrency(e: any) {
    this.setUserValue(e.value ?? null);
  }

  writeValue(obj: any): void {
    this.value = obj;
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
    this.setUserValue(null);
  }

  private setUserValue(code: string | null): void {
    if (this.value === code) return;
    this.value = code;
    this.onChange(code);
    this.onTouched();
  }
}
