import { Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';
import { Dialog } from 'primeng/dialog';

import { BeneficiarySplitComponent } from '../../../shared/beneficiary-split/beneficiary-split.component';
import { CsvImportDraftStore } from '../../csv-import-draft.store';

@Component({
  selector: 'app-beneficiary-split-dialog',
  imports: [ReactiveFormsModule, TranslatePipe, ButtonDirective, Dialog, BeneficiarySplitComponent],
  templateUrl: './beneficiary-split-dialog.component.html',
})
export class BeneficiarySplitDialogComponent {
  readonly store = inject(CsvImportDraftStore);
}
