import { describe, expect, it } from 'vitest';

import { CsvImportReferenceIndex } from './csv-import-reference-index';

describe('CsvImportReferenceIndex', () => {
  const wallets = [
    { id: 'wallet-primary', name: 'Conta São João' },
    { id: 'wallet-other', name: 'Outra conta' },
    { id: 'wallet-duplicate-a', name: 'Conta conjunta' },
    { id: 'wallet-duplicate-b', name: 'conta CONJUNTA' },
  ] as never;
  const groups = [
    { id: 'group-primary', name: 'Família São João' },
    { id: 'group-other', name: 'Outro grupo' },
    { id: 'group-duplicate-a', name: 'Casa compartilhada' },
    { id: 'group-duplicate-b', name: 'CASA COMPARTILHADA' },
  ] as never;
  const categories = [
    { id: 'category-primary', conceptId: 'concept-primary', name: 'Alimentação São João' },
    { id: 'category-other', conceptId: 'concept-other', name: 'Outra categoria' },
    { id: 'category-duplicate-a', conceptId: 'concept-duplicate-a', name: 'Casa e Jardim' },
    { id: 'category-duplicate-b', conceptId: 'concept-duplicate-b', name: 'CASA E JARDIM' },
  ] as never;

  function index(): CsvImportReferenceIndex {
    return new CsvImportReferenceIndex(wallets, groups, [], new Map([['group-primary', categories]]));
  }

  describe('origin cascade', () => {
    it.each([
      ['valid id', 'wallet-primary', undefined, 'wallet-primary'],
      ['invalid id then valid name', 'missing', 'Outra conta', 'wallet-other'],
      ['empty id then valid name', '', 'Outra conta', 'wallet-other'],
      ['normalized case and accent name', undefined, 'CONTA SAO JOAO', 'wallet-primary'],
    ])('%s', (_case, id, name, expected) => {
      expect(index().resolveWalletItem(id, name)?.id).toBe(expected);
    });

    it('leaves an ambiguous normalized name unresolved', () => {
      expect(index().resolveWalletItem(undefined, 'conta conjunta')).toBeUndefined();
    });

    it('gives a valid id precedence over a conflicting name', () => {
      expect(index().resolveWalletItem('wallet-primary', 'Outra conta')?.id).toBe('wallet-primary');
    });
  });

  describe('group cascade', () => {
    it.each([
      ['valid id', 'group-primary', undefined, 'group-primary'],
      ['invalid id then valid name', 'missing', 'Outro grupo', 'group-other'],
      ['empty id then valid name', '', 'Outro grupo', 'group-other'],
      ['normalized case and accent name', undefined, 'FAMILIA SAO JOAO', 'group-primary'],
    ])('%s', (_case, id, name, expected) => {
      expect(index().resolveGroup(id, name)?.id).toBe(expected);
    });

    it('leaves an ambiguous normalized name unresolved', () => {
      expect(index().resolveGroup(undefined, 'casa compartilhada')).toBeUndefined();
    });

    it('gives a valid id precedence over a conflicting name', () => {
      expect(index().resolveGroup('group-primary', 'Outro grupo')?.id).toBe('group-primary');
    });
  });

  describe('category cascade', () => {
    it.each([
      ['valid id', 'category-primary', undefined, undefined, 'category-primary'],
      ['invalid id then valid concept', 'missing', 'concept-other', undefined, 'category-other'],
      ['empty id then valid concept', '', 'concept-other', undefined, 'category-other'],
      ['invalid id and concept then valid name', 'missing', 'missing', 'Outra categoria', 'category-other'],
      ['empty id and concept then valid name', '', '', 'Outra categoria', 'category-other'],
      ['normalized case and accent name', undefined, undefined, 'ALIMENTACAO SAO JOAO', 'category-primary'],
    ])('%s', (_case, id, conceptId, name, expected) => {
      expect(index().resolveCategory('group-primary', id, conceptId, name)?.id).toBe(expected);
    });

    it('leaves an ambiguous normalized name unresolved', () => {
      expect(index().resolveCategory('group-primary', undefined, undefined, 'casa e jardim')).toBeUndefined();
    });

    it('gives a valid id precedence over conflicting concept and name values', () => {
      expect(index().resolveCategory('group-primary', 'category-primary', 'concept-other', 'Outra categoria')?.id).toBe('category-primary');
    });
  });
});
