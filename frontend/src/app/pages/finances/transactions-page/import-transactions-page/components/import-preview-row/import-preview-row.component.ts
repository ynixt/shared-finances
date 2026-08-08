import { CommonModule } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Tooltip } from 'primeng/tooltip';

import { CurrencySelectorComponent } from '../../../../../../components/currency-selector/currency-selector.component';
import { DatePickerComponent } from '../../../../../../components/date-picker/date-picker.component';
import { WalletItemSearchResponseDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { LocalCurrencyPipe } from '../../../../../../pipes/local-currency.pipe';
import { LocalDatePipe } from '../../../../../../pipes/local-date.pipe';
import { CategoryPickerComponent } from '../../../../components/item-picker/category-picker/category-picker.component';
import { WalletItemPickerComponent } from '../../../../components/item-picker/wallet-item-picker/wallet-item-picker.component';
import { CsvImportDraftStore } from '../../csv-import-draft.store';
import { ImportPreviewRow } from '../../import-transactions.models';

@Component({
  selector: 'app-import-preview-row',
  imports: [
    FormsModule,
    CommonModule,
    TranslatePipe,
    ButtonDirective,
    InputNumber,
    Tooltip,
    CurrencySelectorComponent,
    DatePickerComponent,
    InputText,
    Select,
    CategoryPickerComponent,
    WalletItemPickerComponent,
    LocalCurrencyPipe,
    LocalDatePipe,
  ],
  templateUrl: './import-preview-row.component.html',
  styleUrl: './import-preview-row.component.scss',
})
export class ImportPreviewRowComponent {
  readonly previewRow = input.required<ImportPreviewRow>();
  readonly store = inject(CsvImportDraftStore);
  private readonly translateService = inject(TranslateService);

  get groupOptions() {
    return [
      {
        id: '',
        name: this.translateService.instant('financesPage.transactionsPage.importPage.fields.group'),
      },
      ...this.store.groups,
    ];
  }

  private dateControlValue?: { source: string; value: Date };
  private billControlValue?: { source: string; value: Date };

  get row(): ImportPreviewRow {
    return this.previewRow();
  }

  rowDateValue(): Date | undefined {
    this.dateControlValue = this.controlDate(this.row.date, this.dateControlValue);
    return this.dateControlValue?.value;
  }

  rowDateChanged(value: Date | null | undefined): void {
    void this.store.rowDateChanged(this.row, value == null ? '' : dayjs(value).format('YYYY-MM-DD'));
  }

  rowOriginChanged(origin: WalletItemSearchResponseDto | null | undefined): void {
    this.row.walletItemId = origin?.id;
    void this.store.rowOriginChanged(this.row);
  }

  rowGroupChanged(groupId: string | null | undefined): void {
    this.row.groupId = groupId ?? undefined;
    void this.store.rowGroupChanged(this.row);
  }

  rowBillValue(): Date | undefined {
    this.billControlValue = this.controlDate(this.row.billDate, this.billControlValue);
    return this.billControlValue?.value;
  }

  rowBillChanged(value: Date | null | undefined): void {
    this.store.setBillMonth(this.row, value == null ? '' : dayjs(value).format('YYYY-MM'));
  }

  private controlDate(source: string | undefined, cached: { source: string; value: Date } | undefined) {
    if (source == null || source === '') return undefined;
    if (cached?.source === source) return cached;
    const parsed = dayjs(source);
    return parsed.isValid() ? { source, value: parsed.toDate() } : undefined;
  }
}
