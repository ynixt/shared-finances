import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';

import { describe, expect, it } from 'vitest';

import { ImportDraftStore } from '../../import-draft.store';
import { ImportFixedValuesComponent } from './import-fixed-values.component';

describe('ImportFixedValuesComponent', () => {
  it('lists only real groups and leaves the selector empty when no group is selected', async () => {
    const groups = [{ id: 'group-1', name: 'Teste' }];

    await TestBed.configureTestingModule({
      imports: [ImportFixedValuesComponent],
      providers: [
        {
          provide: ImportDraftStore,
          useValue: { groups, fixedGroupId: undefined },
        },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
      ],
    })
      .overrideComponent(ImportFixedValuesComponent, { set: { template: '' } })
      .compileComponents();

    const component = TestBed.createComponent(ImportFixedValuesComponent).componentInstance;

    expect(component.groupOptions).toEqual(groups);
    expect(component.groupOptions).not.toContainEqual(expect.objectContaining({ id: '' }));
    expect(component.selectedGroupId).toBeNull();
  });
});
