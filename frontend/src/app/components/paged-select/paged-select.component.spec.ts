import '@angular/compiler';
import { TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { PagedMultiSelectComponent } from '../paged-multi-select/paged-multi-select.component';
import { PagedSelectComponent } from './paged-select.component';

describe('paged select control value accessors', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('does not emit a change when Angular writes a value into a paged select', async () => {
    await TestBed.configureTestingModule({ imports: [PagedSelectComponent] }).compileComponents();
    const component = TestBed.createComponent(PagedSelectComponent).componentInstance;
    const changed = vi.fn();
    const selected = { id: 'wallet-1', name: 'Conta' };

    component.registerOnChange(changed);
    component.writeValue(selected);

    expect(component.value).toBe(selected);
    expect(changed).not.toHaveBeenCalled();

    const replacement = { id: 'wallet-2', name: 'Cartão' };
    component.onSelectionChange({ value: replacement } as never);

    expect(component.value).toBe(replacement);
    expect(changed).toHaveBeenCalledOnce();
    expect(changed).toHaveBeenCalledWith(replacement);
  });

  it('does not emit a change when Angular writes values into a paged multi-select', async () => {
    await TestBed.configureTestingModule({ imports: [PagedMultiSelectComponent] }).compileComponents();
    const component = TestBed.createComponent(PagedMultiSelectComponent).componentInstance;
    const changed = vi.fn();
    const selected = [{ id: 'member-1', name: 'Pessoa' }];

    component.registerOnChange(changed);
    component.writeValue(selected);

    expect(component.value).toBe(selected);
    expect(changed).not.toHaveBeenCalled();

    const replacement = [{ id: 'member-2', name: 'Outra pessoa' }];
    component.onSelectionChange({ value: replacement } as never);

    expect(component.value).toBe(replacement);
    expect(changed).toHaveBeenCalledOnce();
    expect(changed).toHaveBeenCalledWith(replacement);
  });
});
