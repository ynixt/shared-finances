import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Tooltip } from 'primeng/tooltip';

import { CurrencySelectorComponent } from '../../../../../../components/currency-selector/currency-selector.component';
import { DatePickerComponent } from '../../../../../../components/date-picker/date-picker.component';
import { WalletItemSearchResponseDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryPickerComponent } from '../../../../components/item-picker/category-picker/category-picker.component';
import { WalletItemPickerComponent } from '../../../../components/item-picker/wallet-item-picker/wallet-item-picker.component';
import { CsvImportDraftStore } from '../../csv-import-draft.store';
import { CsvColumnField } from '../../csv-statement-parser';

@Component({
  selector: 'app-import-fixed-values',
  imports: [
    FormsModule,
    TranslatePipe,
    Tooltip,
    CurrencySelectorComponent,
    DatePickerComponent,
    InputNumber,
    InputText,
    Select,
    CategoryPickerComponent,
    WalletItemPickerComponent,
  ],
  templateUrl: './import-fixed-values.component.html',
  styleUrl: './import-fixed-values.component.scss',
})
export class ImportFixedValuesComponent {
  readonly store = inject(CsvImportDraftStore);
  private readonly translateService = inject(TranslateService);

  get groupOptions() {
    return this.store.groups;
  }

  get selectedGroupId(): string | null {
    return this.store.fixedGroupId ?? null;
  }

  get confirmationOptions() {
    return [
      { name: this.translateService.instant('financesPage.transactionsPage.importPage.common.yes'), value: true },
      { name: this.translateService.instant('financesPage.transactionsPage.importPage.common.no'), value: false },
    ];
  }

  get fixedValueCurrency(): string {
    const currency = this.store.fixedValues.currency;
    return typeof currency === 'string' && currency !== '' ? currency : this.store.defaultCurrency;
  }

  private readonly dateControlValues = new Map<'bill' | 'date', { source: string; value: Date }>();

  fixedOrigin(): WalletItemSearchResponseDto | undefined {
    return this.store.fixedOrigin;
  }

  fixedOriginChanged(origin: WalletItemSearchResponseDto | null | undefined): void {
    void this.store.setFixedValue('origin', origin?.id ?? '');
  }

  fixedDateValue(field: 'bill' | 'date'): Date | undefined {
    const source = String(this.store.fixedValues[field] ?? '');
    if (source === '') return undefined;

    const cached = this.dateControlValues.get(field);
    if (cached?.source === source) return cached.value;

    const parsed = dayjs(field === 'bill' ? `${source}-01` : source);
    if (!parsed.isValid()) return undefined;
    const value = parsed.toDate();
    this.dateControlValues.set(field, { source, value });
    return value;
  }

  fixedDateChanged(field: 'bill' | 'date', value: Date | null | undefined): void {
    const formatted = value == null ? '' : dayjs(value).format(field === 'bill' ? 'YYYY-MM' : 'YYYY-MM-DD');
    void this.store.setFixedValue(field, formatted);
  }

  groupChanged(value: string | null | undefined): void {
    void this.store.setFixedValue('group', value ?? '');
  }

  fixedValueChanged(field: CsvColumnField, value: string | boolean | null | undefined): void {
    void this.store.setFixedValue(field, value ?? '');
  }

  fixedNumberValue(field: CsvColumnField): number | null {
    const rawValue = this.store.fixedValues[field];
    const value = Number(rawValue);
    return rawValue !== '' && Number.isFinite(value) ? value : null;
  }

  fixedNumberChanged(field: CsvColumnField, value: number | null | undefined): void {
    void this.store.setFixedValue(field, value ?? '');
  }
}
