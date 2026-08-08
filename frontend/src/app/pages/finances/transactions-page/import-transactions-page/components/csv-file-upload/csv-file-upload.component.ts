import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';

import { CsvImportDraftStore } from '../../csv-import-draft.store';

@Component({
  selector: 'app-csv-file-upload',
  imports: [CommonModule, TranslatePipe, ButtonDirective],
  templateUrl: './csv-file-upload.component.html',
  styleUrl: './csv-file-upload.component.scss',
})
export class CsvFileUploadComponent {
  readonly store = inject(CsvImportDraftStore);
}
