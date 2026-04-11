import { HttpClient } from '@angular/common/http';
import { Component, OnInit, forwardRef, input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { ScrollerOptions } from 'primeng/api';
import { Select } from 'primeng/select';

export interface CurrencyItem {
  code: string;
  name?: string;
  symbol: string;
}

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
  assetsUrl = input('/public/currencies.json');

  currencies: CurrencyItem[] = [];
  value: string | null = null;

  scrollerOptions: ScrollerOptions = {
    showLoader: true,
    scrollWidth: '240px',
  };

  private onChange = (_: any) => {};
  private onTouched = () => {};
  disabled = false;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadCurrencies();
  }

  private loadCurrencies() {
    this.http.get<Record<string, string>>(this.assetsUrl(), { responseType: 'json' as const }).subscribe({
      next: data => {
        this.currencies = Object.entries(data)
          .map(([code, name]) => ({
            code: code.toUpperCase(),
            name: name || undefined,
            symbol: this.getIntlSymbol(code.toUpperCase()),
            all: code.toUpperCase() + ' ' + name + ' ' + this.getIntlSymbol(code.toUpperCase()),
          }))
          .sort((a, b) => a.code.localeCompare(b.code));
      },
      error: () => {
        this.currencies = [];
      },
    });
  }

  getIntlSymbol(code: string, locale = 'en-US') {
    try {
      const parts = new Intl.NumberFormat(locale, { style: 'currency', currency: code }).formatToParts(0);
      const sym = parts.find(p => p.type === 'currency')?.value;
      return sym ?? code;
    } catch {
      return code;
    }
  }

  onSelectCurrency(e: any) {
    const item = e.value;

    if (!item) {
      this.writeValue(null);
      return;
    }
    this.writeValue(item);
    this.onTouched();
  }

  writeValue(obj: any): void {
    this.value = obj;
    this.propagateChange(obj);
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

  private propagateChange(code: string | null) {
    this.onChange(code);
  }

  clearSelection() {
    this.writeValue(null);
    this.propagateChange(null);
  }
}
