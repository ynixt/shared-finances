import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { Select } from 'primeng/select';
import { Tooltip } from 'primeng/tooltip';

import { CsvImportDraftStore } from '../../csv-import-draft.store';
import { CsvColumnField } from '../../csv-statement-parser';
import { ImportDraftStore } from '../../import-draft.store';

interface MappingSelectOption {
  name: string;
  value: string;
}

@Component({
  selector: 'app-csv-column-mapping',
  imports: [FormsModule, TranslatePipe, Tooltip, Select],
  templateUrl: './csv-column-mapping.component.html',
  styleUrl: './csv-column-mapping.component.scss',
})
export class CsvColumnMappingComponent {
  readonly store = inject(ImportDraftStore);
  private readonly csvStore = inject(CsvImportDraftStore);
  private readonly translateService = inject(TranslateService);

  mappingChanged(field: CsvColumnField, value: string | null | undefined): void {
    void this.store.setMapping(field, value ?? '');
  }

  get mappingFields() {
    return this.store.fileFormat === 'OFX'
      ? this.store.mappingOptions.filter(option => option.field === 'bill')
      : this.store.mappingOptions;
  }

  get titleKey(): string {
    return this.store.fileFormat === 'OFX'
      ? 'financesPage.transactionsPage.importPage.mapping.billTitle'
      : 'financesPage.transactionsPage.importPage.mapping.title';
  }

  get hintKey(): string {
    return this.store.fileFormat === 'OFX'
      ? 'financesPage.transactionsPage.importPage.mapping.billHint'
      : 'financesPage.transactionsPage.importPage.mapping.hint';
  }

  mappingOptions(field: CsvColumnField): MappingSelectOption[] {
    const options: MappingSelectOption[] = [];
    if (field === 'bill') {
      options.push({
        name: this.translateService.instant('financesPage.transactionsPage.importPage.mapping.billFromDate'),
        value: this.store.billFromDateMappingValue,
      });
    }
    if (field !== 'origin' && field !== 'bill') {
      options.push({
        name: this.translateService.instant('financesPage.transactionsPage.importPage.mapping.autoDetect'),
        value: '',
      });
    }
    options.push({
      name: this.translateService.instant('financesPage.transactionsPage.importPage.mapping.fixedValue'),
      value: this.store.fixedMappingValue,
    });
    if (this.store.fileFormat !== 'OFX') {
      options.push(...this.csvStore.headers.map(header => ({ name: header, value: header })));
    }
    return options;
  }
}
