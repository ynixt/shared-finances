export type ExportFilterSelection = {
  walletItemIds: string[];
  categoryIds: string[];
};

export function retainAssociatedExportSelections(
  selection: ExportFilterSelection,
  walletItems: ReadonlyArray<{ id: string }>,
  categories: ReadonlyArray<{ id: string }>,
): ExportFilterSelection {
  const walletIds = new Set(walletItems.map(item => item.id));
  const categoryIds = new Set(categories.map(category => category.id));
  return {
    walletItemIds: selection.walletItemIds.filter(id => walletIds.has(id)),
    categoryIds: selection.categoryIds.filter(id => categoryIds.has(id)),
  };
}
