import { HttpClient } from '@angular/common/http';
import { Component, Input, OnInit, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { ScrollerOptions } from 'primeng/api';
import { Select } from 'primeng/select';

export interface CurrencyItem {
  code: string;
  name?: string;
  symbol: string;
}

interface DataHubCurrency {
  Entity: string;
  Currency: string;
  AlphabeticCode?: string;
  NumericCode: string;
  MinorUnit: string;
  WithdrawalDate: string;
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
  @Input() placeholder = '';
  @Input() assetsUrl = '/public/currencies.json';

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
    this.http.get<DataHubCurrency[]>(this.assetsUrl, { responseType: 'json' as const }).subscribe({
      next: list => {
        const codesAlreadyRead = new Set<string>();

        this.currencies = list
          .filter(it => it.AlphabeticCode != null && !codesAlreadyRead.has(it.AlphabeticCode) && codesAlreadyRead.add(it.AlphabeticCode))
          .map(it => ({
            code: it.AlphabeticCode!!.toUpperCase(),
            name: it.Currency,
            symbol: this.getIntlSymbol(it.AlphabeticCode!!.toUpperCase()),
          }))
          .sort((a, b) => a.code.localeCompare(b.code));
      },
      error: () => {
        this.currencies = [];
      },
    });
  }

  getIntlSymbol(code: string, locale = 'en-US') {
    const parts = new Intl.NumberFormat(locale, { style: 'currency', currency: code }).formatToParts(0);
    const sym = parts.find(p => p.type === 'currency')?.value;
    return sym ?? code;
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
