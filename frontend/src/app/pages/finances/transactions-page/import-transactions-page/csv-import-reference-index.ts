import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { normalizeHeader } from './csv-statement-parser';

interface NamedReference {
  id: string;
  name: string;
}

interface NamedReferenceIndex<T extends NamedReference> {
  byId: Map<string, T>;
  byName: Map<string, T>;
}

interface CategoryReferenceIndex extends NamedReferenceIndex<CategoryDto> {
  byConceptId: Map<string, CategoryDto>;
}

export class CsvImportReferenceIndex {
  private readonly walletItems: NamedReferenceIndex<WalletItemSearchResponseDto>;
  private readonly groups: NamedReferenceIndex<GroupWithRoleDto>;
  private readonly categoriesByScope: Map<string, CategoryReferenceIndex>;

  constructor(
    walletItems: readonly WalletItemSearchResponseDto[],
    groups: readonly GroupWithRoleDto[],
    personalCategories: readonly CategoryDto[],
    groupCategories: ReadonlyMap<string, readonly CategoryDto[]>,
  ) {
    this.walletItems = buildNamedIndex(walletItems);
    this.groups = buildNamedIndex(groups);
    this.categoriesByScope = new Map([
      ['', buildCategoryIndex(personalCategories)],
      ...[...groupCategories].map(([groupId, categories]) => [groupId, buildCategoryIndex(categories)] as const),
    ]);
  }

  resolveWalletItem(id?: string, name?: string): WalletItemSearchResponseDto | undefined {
    return this.walletItems.byId.get(normalizeId(id)) ?? this.walletItems.byName.get(normalizeName(name));
  }

  resolveGroup(id?: string, name?: string): GroupWithRoleDto | undefined {
    return this.groups.byId.get(normalizeId(id)) ?? this.groups.byName.get(normalizeName(name));
  }

  resolveCategory(groupId?: string, id?: string, conceptId?: string, name?: string): CategoryDto | undefined {
    const index = this.categoriesByScope.get(groupId ?? '');
    return index?.byId.get(normalizeId(id)) ?? index?.byConceptId.get(normalizeId(conceptId)) ?? index?.byName.get(normalizeName(name));
  }
}

function buildNamedIndex<T extends NamedReference>(values: readonly T[]): NamedReferenceIndex<T> {
  return {
    byId: new Map(values.map(value => [normalizeId(value.id), value])),
    byName: buildUniqueIndex(values, value => normalizeName(value.name)),
  };
}

function buildCategoryIndex(values: readonly CategoryDto[]): CategoryReferenceIndex {
  return {
    ...buildNamedIndex(values),
    byConceptId: buildUniqueIndex(values, value => normalizeId(value.conceptId)),
  };
}

function buildUniqueIndex<T>(values: readonly T[], keyOf: (value: T) => string): Map<string, T> {
  const unique = new Map<string, T>();
  const ambiguous = new Set<string>();
  for (const value of values) {
    const key = keyOf(value);
    if (key === '' || ambiguous.has(key)) continue;
    if (unique.has(key)) {
      unique.delete(key);
      ambiguous.add(key);
    } else {
      unique.set(key, value);
    }
  }
  return unique;
}

function normalizeId(value?: string): string {
  return value?.trim().toLowerCase() ?? '';
}

function normalizeName(value?: string): string {
  const normalized = normalizeHeader(value ?? '');
  return normalized === '-' ? '' : normalized;
}
