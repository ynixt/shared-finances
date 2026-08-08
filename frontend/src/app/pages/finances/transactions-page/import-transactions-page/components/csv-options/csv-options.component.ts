import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';

import { CsvImportDraftStore } from '../../csv-import-draft.store';
import { ImportDraftStore } from '../../import-draft.store';

@Component({
  selector: 'app-csv-options',
  imports: [FormsModule, TranslatePipe, InputText, Select],
  templateUrl: './csv-options.component.html',
  styleUrl: './csv-options.component.scss',
})
export class CsvOptionsComponent {
  readonly csvStore = inject(CsvImportDraftStore);
  readonly store = inject(ImportDraftStore);
  private readonly translateService = inject(TranslateService);

  get decimalSeparatorOptions() {
    return [
      { name: this.translateService.instant('financesPage.transactionsPage.importPage.csvOptions.decimalPoint'), value: '.' },
      { name: this.translateService.instant('financesPage.transactionsPage.importPage.csvOptions.decimalComma'), value: ',' },
    ];
  }

  get dateFormatOptions() {
    return this.csvStore.dateFormats.map(option => ({
      name: this.translateService.instant(option.labelKey),
      value: option.value,
    }));
  }
}
