import { TestBed } from '@angular/core/testing';

import { MessageService } from 'primeng/api';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ImportService } from '../../services/import.service';
import { CsvImportSubmissionService } from './csv-import-submission.service';
import { ImportPreviewRow } from './import-transactions.models';

describe('CsvImportSubmissionService', () => {
  const imports = { create: vi.fn() };

  beforeEach(() => {
    vi.clearAllMocks();
    imports.create.mockResolvedValue({ id: 'batch', status: 'QUEUED' });
    TestBed.configureTestingModule({
      providers: [
        CsvImportSubmissionService,
        { provide: ImportService, useValue: imports },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
  });

  it('validates the snapshot and submits the existing import contract', async () => {
    const service = TestBed.inject(CsvImportSubmissionService);
    const invalid = await service.submit({ canShowPreview: false, fileHash: 'hash', rows: [] }, key => key);
    const result = await service.submit(
      { canShowPreview: true, file: new File(['csv'], 'statement.csv'), fileHash: 'hash', rows: [previewRow()] },
      key => key,
    );

    expect(invalid).toEqual({ errorKey: 'errors.fixedOriginRequired' });
    expect(result.batch).toEqual(expect.objectContaining({ id: 'batch' }));
    expect(imports.create).toHaveBeenCalledWith(
      expect.objectContaining({
        fileHash: 'hash',
        fileName: 'statement.csv',
        lines: [expect.objectContaining({ walletItemId: 'wallet', value: 10, date: '2026-08-08' })],
      }),
    );
  });
});

function previewRow(): ImportPreviewRow {
  return {
    raw: {},
    index: 0,
    included: true,
    duplicate: false,
    date: '2026-08-08',
    value: 10,
    currencySource: 'FALLBACK',
    convertedValue: 10,
    convertedValueOverridden: false,
    conversionLoading: false,
    conversionTargetCurrency: 'BRL',
    createPreviousInstallments: false,
    createFollowingInstallments: false,
    confirmed: true,
    beneficiaries: [],
    walletItemId: 'wallet',
  };
}
