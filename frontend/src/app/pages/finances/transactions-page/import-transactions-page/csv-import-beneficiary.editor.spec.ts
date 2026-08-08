import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CsvImportBeneficiaryEditor } from './csv-import-beneficiary.editor';
import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { CsvImportRowResolver } from './csv-import-row.resolver';
import { ImportPreviewRow } from './import-transactions.models';

describe('CsvImportBeneficiaryEditor', () => {
  const user = { id: 'user', email: 'user@example.com', firstName: 'User', lastName: 'Test', label: 'User Test' };
  const resolver = {
    ensureDefaultBeneficiary: vi.fn((row: ImportPreviewRow) => {
      if (row.beneficiaries.length === 0) row.beneficiaries = [{ userId: user.id, email: user.email, benefitPercent: 100 }];
    }),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CsvImportBeneficiaryEditor,
        FormBuilder,
        { provide: CsvImportCatalogStore, useValue: { ensureGroupMembers: () => Promise.resolve([user]) } },
        { provide: CsvImportRowResolver, useValue: resolver },
      ],
    });
  });

  it('opens with resolved members and applies a valid split to the row', async () => {
    const editor = TestBed.inject(CsvImportBeneficiaryEditor);
    const row = previewRow();

    await editor.open(row);
    editor.addLeg();
    editor.form.get('primaryBeneficiaryUser')?.setValue(user);
    const extra = editor.form.get('extraBeneficiaryLegs.0');
    extra?.get('user')?.setValue({ ...user, id: 'other', email: 'other@example.com' });
    editor.save();

    expect(editor.visible).toBe(false);
    expect(row.beneficiaries).toEqual([
      { userId: 'user', benefitPercent: 50 },
      { userId: 'other', benefitPercent: 50 },
    ]);
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
    groupId: 'group',
    beneficiaries: [],
  };
}
