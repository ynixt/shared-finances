import { Component, inject, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Paginator } from 'primeng/paginator';
import { Select } from 'primeng/select';

import { ImportBatchDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/imports';
import { LocalCurrencyPipe } from '../../../../../../pipes/local-currency.pipe';
import { ImportDraftStore } from '../../import-draft.store';
import { BeneficiarySplitDialogComponent } from '../beneficiary-split-dialog/beneficiary-split-dialog.component';
import { ImportPreviewTableComponent } from '../import-preview-table/import-preview-table.component';

@Component({
  selector: 'app-import-preview',
  imports: [
    FormsModule,
    TranslatePipe,
    ButtonDirective,
    Paginator,
    LocalCurrencyPipe,
    InputText,
    Select,
    ImportPreviewTableComponent,
    BeneficiarySplitDialogComponent,
  ],
  templateUrl: './import-preview.component.html',
  styleUrl: './import-preview.component.scss',
})
export class ImportPreviewComponent {
  readonly store = inject(ImportDraftStore);
  readonly batchAccepted = output<ImportBatchDto>();
  private readonly translateService = inject(TranslateService);

  get categoryFilterOptions() {
    return [
      {
        id: '',
        name: this.translateService.instant('financesPage.transactionsPage.importPage.preview.allCategories'),
      },
      ...this.store.categories,
    ];
  }

  categoryFilterChanged(categoryId: string | null | undefined): void {
    this.store.categoryFilter = categoryId ?? '';
    this.store.resetPreviewPagination();
  }

  async submit(): Promise<void> {
    const batch = await this.store.submit();
    if (batch != null) this.batchAccepted.emit(batch);
  }
}
