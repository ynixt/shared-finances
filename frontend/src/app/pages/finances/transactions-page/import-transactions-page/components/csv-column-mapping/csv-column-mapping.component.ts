import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { Select } from 'primeng/select';
import { Tooltip } from 'primeng/tooltip';

import { CsvImportDraftStore } from '../../csv-import-draft.store';
import { CsvColumnField } from '../../csv-statement-parser';

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
  readonly store = inject(CsvImportDraftStore);
  private readonly translateService = inject(TranslateService);

  mappingChanged(field: CsvColumnField, value: string | null | undefined): void {
    void this.store.setMapping(field, value ?? '');
  }

  mappingOptions(field: CsvColumnField): MappingSelectOption[] {
    const options: MappingSelectOption[] = [];
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
    options.push(...this.store.headers.map(header => ({ name: header, value: header })));
    return options;
  }
}
