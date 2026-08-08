import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { WalletItemSearchResponseDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { WalletItemPickerComponent } from '../../../../components/item-picker/wallet-item-picker/wallet-item-picker.component';
import { CsvImportDraftStore } from '../../csv-import-draft.store';
import { ParsedImportSourceStatement } from '../../import-file-source';

@Component({
  selector: 'app-ofx-account-mapping',
  imports: [FormsModule, TranslatePipe, WalletItemPickerComponent],
  templateUrl: './ofx-account-mapping.component.html',
  styleUrl: './ofx-account-mapping.component.scss',
})
export class OfxAccountMappingComponent {
  readonly store = inject(CsvImportDraftStore);

  originChanged(statement: ParsedImportSourceStatement, origin: WalletItemSearchResponseDto | null | undefined): void {
    void this.store.setOfxStatementOrigin(statement, origin);
  }
}
