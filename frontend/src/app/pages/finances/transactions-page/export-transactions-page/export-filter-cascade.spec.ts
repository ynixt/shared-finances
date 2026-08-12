import { retainAssociatedExportSelections } from './export-filter-cascade';

describe('retainAssociatedExportSelections', () => {
  it('drops wallet items and categories that are not associated with the selected group', () => {
    const result = retainAssociatedExportSelections(
      { walletItemIds: ['bank-a', 'card-b'], categoryIds: ['food', 'travel'] },
      [{ id: 'card-b' }],
      [{ id: 'food' }],
    );

    expect(result).toEqual({ walletItemIds: ['card-b'], categoryIds: ['food'] });
  });
});
