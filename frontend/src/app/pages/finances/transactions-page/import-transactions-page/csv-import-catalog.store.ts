import { Injectable, inject } from '@angular/core';

import { firstValueFrom } from 'rxjs';

import { CurrencyCatalogService, CurrencyItem } from '../../../../components/currency-selector/currency-catalog.service';
import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { Page, PageRequest } from '../../../../models/pagination';
import { UserService } from '../../../../services/user.service';
import { GroupService } from '../../services/group.service';
import { ImportService } from '../../services/import.service';
import { WalletItemService } from '../../services/wallet-item.service';
import { UserForBeneficiary } from '../shared/transaction-form/transaction-form.types';
import { CsvImportReferenceIndex } from './csv-import-reference-index';
import { normalizeHeader } from './csv-statement-parser';
import { ImportPreviewRow } from './import-transactions.models';

@Injectable()
export class CsvImportCatalogStore {
  private static readonly CATALOG_PAGE_SIZE = 10;

  private readonly currencyCatalog = inject(CurrencyCatalogService);
  private readonly userService = inject(UserService);
  private readonly walletItemService = inject(WalletItemService);
  private readonly importService = inject(ImportService);
  private readonly groupService = inject(GroupService);

  defaultCurrency = 'USD';
  currencyOptions: string[] = [];
  currencies: CurrencyItem[] = [];
  categories: CategoryDto[] = [];
  groups: GroupWithRoleDto[] = [];
  currentUser?: UserForBeneficiary;

  private storedWalletItems: WalletItemSearchResponseDto[] = [];
  private categoriesByGroup = new Map<string, CategoryDto[]>();
  private walletItemsById = new Map<string, WalletItemSearchResponseDto>();
  private beneficiaryMembersByGroup = new Map<string, UserForBeneficiary[]>();
  private beneficiaryMemberRequestsByGroup = new Map<string, Promise<UserForBeneficiary[]>>();
  private readonly emptyCategories: CategoryDto[] = [];

  get walletItems(): WalletItemSearchResponseDto[] {
    return this.storedWalletItems;
  }

  set walletItems(value: WalletItemSearchResponseDto[]) {
    this.storedWalletItems = value;
    this.walletItemsById = new Map(value.map(item => [item.id, item]));
  }

  async load(): Promise<void> {
    const [walletItems, categoryCatalog, groups, user, currencies] = await Promise.all([
      this.loadEveryPage(request => this.walletItemService.getAllItems(request)),
      this.importService.categoryCatalog(),
      this.groupService.getAllGroups(),
      this.userService.getUser(),
      firstValueFrom(this.currencyCatalog.getCurrencies('/public/currencies.json')),
    ]);
    this.walletItems = walletItems;
    this.categories = this.flattenCategories(categoryCatalog.personal);
    this.groups = groups;
    this.categoriesByGroup = new Map(categoryCatalog.groups.map(group => [group.groupId, this.flattenCategories(group.categories)]));
    this.defaultCurrency = user?.defaultCurrency ?? 'USD';
    this.currencies = currencies;
    this.currencyOptions = currencies.map(currency => currency.code);
    this.currentUser =
      user == null
        ? undefined
        : {
            id: user.id,
            email: user.email,
            firstName: user.firstName,
            lastName: user.lastName,
            photoUrl: user.photoUrl,
            label: `${user.firstName} ${user.lastName} (${user.email})`,
          };
    this.beneficiaryMembersByGroup = new Map(
      categoryCatalog.groups.map(group => [
        group.groupId,
        this.withCurrentUser(
          group.members.map(member => ({
            ...member,
            label: `${member.firstName} ${member.lastName} (${member.email})`,
          })),
        ),
      ]),
    );
  }

  createReferenceIndex(): CsvImportReferenceIndex {
    return new CsvImportReferenceIndex(this.walletItems, this.groups, this.categories, this.categoriesByGroup);
  }

  categoriesFor(groupId?: string): CategoryDto[] {
    return groupId == null || groupId === '' ? this.categories : (this.categoriesByGroup.get(groupId) ?? this.emptyCategories);
  }

  async ensureGroupMembers(groupId: string): Promise<UserForBeneficiary[]> {
    const cached = this.beneficiaryMembersByGroup.get(groupId);
    if (cached != null) return cached;
    const pending = this.beneficiaryMemberRequestsByGroup.get(groupId);
    if (pending != null) return pending;

    const request = this.loadGroupMembers(groupId);
    this.beneficiaryMemberRequestsByGroup.set(groupId, request);
    try {
      return await request;
    } finally {
      this.beneficiaryMemberRequestsByGroup.delete(groupId);
    }
  }

  private async loadGroupMembers(groupId: string): Promise<UserForBeneficiary[]> {
    const results = await this.groupService.findAllMembers(groupId);
    const members = results.map(({ user }) => ({
      ...user,
      label: `${user.firstName} ${user.lastName} (${user.email})`,
    }));
    const completeMembers = this.withCurrentUser(members);
    this.beneficiaryMembersByGroup.set(groupId, completeMembers);
    return completeMembers;
  }

  groupMembers(groupId: string): readonly UserForBeneficiary[] {
    return this.beneficiaryMembersByGroup.get(groupId) ?? [];
  }

  findKnownCurrency(value: string | null | undefined): string | undefined {
    const normalized = normalizeHeader(value ?? '');
    if (normalized === '') return undefined;
    const code = this.currencyOptions.find(candidate => normalizeHeader(candidate) === normalized);
    if (code != null) return code;
    return this.currencies.find(currency => normalizeHeader(currency.name ?? '') === normalized)?.code;
  }

  originFor(row: ImportPreviewRow): WalletItemSearchResponseDto | undefined {
    return row.walletItemId == null ? undefined : this.walletItemsById.get(row.walletItemId);
  }

  findCategoryById(categoryId: string | undefined): CategoryDto | undefined {
    if (categoryId == null) return undefined;
    const personalCategory = this.categories.find(category => category.id === categoryId);
    if (personalCategory != null) return personalCategory;

    for (const categories of this.categoriesByGroup.values()) {
      const groupCategory = categories.find(category => category.id === categoryId);
      if (groupCategory != null) return groupCategory;
    }
    return undefined;
  }

  findCategoryByName(categories: readonly CategoryDto[], name: string | null | undefined): CategoryDto | undefined {
    const normalizedName = normalizeHeader(name ?? '');
    if (normalizedName === '' || normalizedName === '-') return undefined;
    return categories.find(category => normalizeHeader(category.name) === normalizedName);
  }

  findMatchingCategory(row: ImportPreviewRow, previousCategory?: CategoryDto, importedName?: string): CategoryDto | undefined {
    const available = this.categoriesFor(row.groupId);
    if (row.categoryId != null) {
      const sameCategory = available.find(category => category.id === row.categoryId);
      if (sameCategory != null) return sameCategory;
    }
    if (previousCategory != null) {
      const conceptMatch = available.find(category => category.conceptId === previousCategory.conceptId);
      if (conceptMatch != null) return conceptMatch;
      const nameMatch = this.findCategoryByName(available, previousCategory.name);
      if (nameMatch != null) return nameMatch;
    }
    return this.findCategoryByName(available, importedName);
  }

  private async loadEveryPage<T>(loader: (request: PageRequest) => Promise<Page<T>>): Promise<T[]> {
    const items: T[] = [];
    for (let pageNumber = 0; ; pageNumber += 1) {
      const page = await loader({ size: CsvImportCatalogStore.CATALOG_PAGE_SIZE, page: pageNumber, sort: 'name' });
      items.push(...page.content);
      if (page.last === true || page.content.length < CsvImportCatalogStore.CATALOG_PAGE_SIZE) return items;
    }
  }

  private flattenCategories(categories: CategoryDto[]): CategoryDto[] {
    return categories.flatMap(category => [category, ...this.flattenCategories(category.children ?? [])]);
  }

  private withCurrentUser(members: UserForBeneficiary[]): UserForBeneficiary[] {
    if (
      this.currentUser != null &&
      !members.some(member => member.id === this.currentUser?.id || member.email.toLowerCase() === this.currentUser?.email.toLowerCase())
    ) {
      return [this.currentUser, ...members];
    }
    return members;
  }
}
