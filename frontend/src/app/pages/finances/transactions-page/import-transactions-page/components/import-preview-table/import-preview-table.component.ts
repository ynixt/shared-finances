import { Component, input } from '@angular/core';

import { ImportPreviewRow } from '../../import-transactions.models';
import { ImportPreviewRowComponent } from '../import-preview-row/import-preview-row.component';

@Component({
  selector: 'app-import-preview-table',
  imports: [ImportPreviewRowComponent],
  templateUrl: './import-preview-table.component.html',
  styleUrl: './import-preview-table.component.scss',
})
export class ImportPreviewTableComponent {
  readonly rows = input.required<ImportPreviewRow[]>();
}
