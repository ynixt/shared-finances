import { Injectable } from '@angular/core';

import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { ParsedImportSourceStatement } from './import-file-source';
import { OfxImportSourceAdapter } from './ofx-import-source.adapter';
import { ParsedOfx } from './ofx-statement-parser';

@Injectable()
export class OfxImportDraftStore {
  private readonly source = new OfxImportSourceAdapter();

  statements: ParsedImportSourceStatement[] = [];
  statementOrigins: Record<string, string> = {};
  pendingCount = 0;

  parse(bytes: ArrayBuffer, maxLines: number): ParsedOfx {
    const parsed = this.source.parse(bytes, maxLines);
    this.statements = parsed.statements;
    this.pendingCount = parsed.pendingCount;
    this.statementOrigins = {};
    return parsed;
  }

  originFor(statement: ParsedImportSourceStatement, walletItems: WalletItemSearchResponseDto[]): WalletItemSearchResponseDto | undefined {
    return walletItems.find(item => item.id === this.statementOrigins[statement.key]);
  }

  setStatementOrigin(
    statement: ParsedImportSourceStatement,
    origin: WalletItemSearchResponseDto | null | undefined,
    walletItems: WalletItemSearchResponseDto[],
  ): string | undefined {
    const originId = origin != null && walletItems.some(item => item.id === origin.id) ? origin.id : undefined;
    if (originId == null) delete this.statementOrigins[statement.key];
    else this.statementOrigins[statement.key] = originId;
    return originId;
  }

  reset(): void {
    this.statements = [];
    this.statementOrigins = {};
    this.pendingCount = 0;
  }
}
