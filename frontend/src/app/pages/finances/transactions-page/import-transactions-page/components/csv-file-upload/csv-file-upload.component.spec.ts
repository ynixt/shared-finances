import { TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ImportDraftStore } from '../../import-draft.store';
import { CsvFileUploadComponent } from './csv-file-upload.component';

describe('CsvFileUploadComponent plan bound', () => {
  const store: any = {
    importPreferencesLoaded: true,
    maxLines: 100,
    file: null,
    parsing: false,
    hashCheck: null,
    selectFile: vi.fn(),
    removeFile: vi.fn(),
  };

  beforeEach(() => {
    store.maxLines = 100;
    TestBed.configureTestingModule({
      imports: [CsvFileUploadComponent, TranslateModule.forRoot()],
      providers: [{ provide: ImportDraftStore, useValue: store }],
    });
  });

  it('shows the applicable bound before a file is selected', () => {
    const fixture = TestBed.createComponent(CsvFileUploadComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="import-max-lines"]')).not.toBeNull();
  });

  it('shows no numeric plan bound when imports are unbounded', () => {
    store.maxLines = null;
    const fixture = TestBed.createComponent(CsvFileUploadComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="import-max-lines"]')).toBeNull();
  });
});
