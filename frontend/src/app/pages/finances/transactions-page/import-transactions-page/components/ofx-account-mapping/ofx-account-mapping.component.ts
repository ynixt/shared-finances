import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { WalletItemSearchResponseDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { WalletItemPickerComponent } from '../../../../components/item-picker/wallet-item-picker/wallet-item-picker.component';
import { ImportDraftStore } from '../../import-draft.store';
import { ParsedImportSourceStatement } from '../../import-file-source';
import { OfxImportDraftStore } from '../../ofx-import-draft.store';

@Component({
  selector: 'app-ofx-account-mapping',
  imports: [FormsModule, TranslatePipe, WalletItemPickerComponent],
  templateUrl: './ofx-account-mapping.component.html',
  styleUrl: './ofx-account-mapping.component.scss',
})
export class OfxAccountMappingComponent {
  readonly store = inject(ImportDraftStore);
  readonly ofxStore = inject(OfxImportDraftStore);

  statementOrigin(statement: ParsedImportSourceStatement): WalletItemSearchResponseDto | undefined {
    return this.ofxStore.originFor(statement, this.store.walletItems);
  }

  originChanged(statement: ParsedImportSourceStatement, origin: WalletItemSearchResponseDto | null | undefined): void {
    void this.store.setOfxStatementOrigin(statement, origin);
  }
}
