import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { faDownload } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ImportBatchDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/imports';
import { FinancesTitleBarComponent, FinancesTitleBarExtraButton } from '../../components/finances-title-bar/finances-title-bar.component';
import { CsvColumnMappingComponent } from './components/csv-column-mapping/csv-column-mapping.component';
import { CsvFileUploadComponent } from './components/csv-file-upload/csv-file-upload.component';
import { ImportFixedValuesComponent } from './components/csv-fixed-values/import-fixed-values.component';
import { CsvOptionsComponent } from './components/csv-options/csv-options.component';
import { ImportHistoryComponent } from './components/import-history/import-history.component';
import { ImportPreviewComponent } from './components/import-preview/import-preview.component';
import { OfxAccountMappingComponent } from './components/ofx-account-mapping/ofx-account-mapping.component';
import { CsvImportBeneficiaryEditor } from './csv-import-beneficiary.editor';
import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { CsvImportConversionService } from './csv-import-conversion.service';
import { CsvImportDraftStore } from './csv-import-draft.store';
import { CsvImportDuplicateService } from './csv-import-duplicate.service';
import { CsvImportRowResolver } from './csv-import-row.resolver';
import { CsvImportSubmissionService } from './csv-import-submission.service';
import { LocalizedCsvTemplate, buildLocalizedCsvTemplate } from './csv-template';
import { ImportBatchRemovedEvent } from './import-transactions.models';

@Component({
  selector: 'app-import-transactions-page',
  imports: [
    CommonModule,
    TranslatePipe,
    FinancesTitleBarComponent,
    CsvFileUploadComponent,
    CsvOptionsComponent,
    CsvColumnMappingComponent,
    ImportFixedValuesComponent,
    ImportPreviewComponent,
    ImportHistoryComponent,
    OfxAccountMappingComponent,
  ],
  providers: [
    CsvImportCatalogStore,
    CsvImportRowResolver,
    CsvImportConversionService,
    CsvImportDuplicateService,
    CsvImportBeneficiaryEditor,
    CsvImportSubmissionService,
    CsvImportDraftStore,
  ],
  templateUrl: './import-transactions-page.component.html',
  styleUrl: './import-transactions-page.component.scss',
})
export class ImportTransactionsPageComponent implements OnInit {
  readonly store = inject(CsvImportDraftStore);
  private readonly translateService = inject(TranslateService);

  readonly titleBarButtons: FinancesTitleBarExtraButton[] = [
    {
      click: () => this.downloadCsvTemplate(),
      rounded: true,
      text: false,
      tooltip: 'financesPage.transactionsPage.importPage.downloadTemplate',
      icon: faDownload,
    },
  ];
  acceptedBatch?: ImportBatchDto;

  ngOnInit(): void {
    void this.store.initialize();
  }

  createCsvTemplate(): LocalizedCsvTemplate {
    return buildLocalizedCsvTemplate(key => this.translateService.instant(key));
  }

  downloadCsvTemplate(): void {
    const template = this.createCsvTemplate();
    const url = URL.createObjectURL(new Blob([template.content], { type: 'text/csv;charset=utf-8' }));
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = template.fileName;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    setTimeout(() => URL.revokeObjectURL(url));
  }

  batchAccepted(batch: ImportBatchDto): void {
    this.acceptedBatch = batch;
  }

  batchRemoved(event: ImportBatchRemovedEvent): void {
    this.store.clearHashCheckFor(event.fileHash);
  }
}
