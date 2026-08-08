import { ImportSourceAdapter } from './import-file-source';
import { ParsedOfx, parseOfx } from './ofx-statement-parser';

export class OfxImportSourceAdapter implements ImportSourceAdapter<ParsedOfx> {
  readonly format = 'OFX' as const;

  parse(bytes: ArrayBuffer, maxLines: number): ParsedOfx {
    return parseOfx(bytes, maxLines);
  }
}
