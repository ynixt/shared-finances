import { Injectable, inject } from '@angular/core';

import { firstValueFrom } from 'rxjs';

import { CurrencyCatalogService, CurrencyItem } from '../../../../components/currency-selector/currency-catalog.service';
import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { Page, PageRequest } from '../../../../models/pagination';
import { UserService } from '../../../../services/user.service';
import { GroupCategoriesService } from '../../services/group-categories.service';
import { GroupService } from '../../services/group.service';
import { UserCategoriesService } from '../../services/user-categories.service';
import { WalletItemService } from '../../services/wallet-item.service';
import { UserForBeneficiary } from '../shared/transaction-form/transaction-form.types';
import { normalizeHeader } from './csv-statement-parser';
import { ImportPreviewRow } from './import-transactions.models';

@Injectable()
export class CsvImportCatalogStore {
  private readonly currencyCatalog = inject(CurrencyCatalogService);
  private readonly userService = inject(UserService);
  private readonly walletItemService = inject(WalletItemService);
  private readonly userCategoriesService = inject(UserCategoriesService);
  private readonly groupCategoriesService = inject(GroupCategoriesService);
  private readonly groupService = inject(GroupService);

  defaultCurrency = 'USD';
  currencyOptions: string[] = [];
  currencies: CurrencyItem[] = [];
  walletItems: WalletItemSearchResponseDto[] = [];
  categories: CategoryDto[] = [];
  groups: GroupWithRoleDto[] = [];
  currentUser?: UserForBeneficiary;

  private categoriesByGroup = new Map<string, CategoryDto[]>();
  private beneficiaryMembersByGroup = new Map<string, UserForBeneficiary[]>();
  private beneficiaryMemberRequestsByGroup = new Map<string, Promise<UserForBeneficiary[]>>();
  private readonly emptyCategories: CategoryDto[] = [];

  async load(): Promise<void> {
    const [walletPage, categories, groups, user, currencies] = await Promise.all([
      this.walletItemService.getAllItems({ size: 500 }),
      this.loadEveryCategoryPage(request => this.userCategoriesService.getAllCategories({}, request)),
      this.groupService.getAllGroups(),
      this.userService.getUser(),
      firstValueFrom(this.currencyCatalog.getCurrencies('/public/currencies.json')),
    ]);
    this.walletItems = walletPage.content;
    this.categories = categories;
    this.groups = groups;
    this.categoriesByGroup = new Map(
      await Promise.all(groups.map(async group => [group.id, await this.loadGroupCategories(group.id)] as const)),
    );
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
  }

  categoriesFor(groupId?: string): CategoryDto[] {
    return groupId == null || groupId === '' ? this.categories : (this.categoriesByGroup.get(groupId) ?? this.emptyCategories);
  }

  async ensureGroupCategories(groupId: string): Promise<CategoryDto[]> {
    const cached = this.categoriesByGroup.get(groupId);
    if (cached != null) return cached;
    const categories = await this.loadGroupCategories(groupId);
    this.categoriesByGroup.set(groupId, categories);
    return categories;
  }

  async ensureGroupMembers(groupId: string): Promise<UserForBeneficiary[]> {
    const cached = this.beneficiaryMembersByGroup.get(groupId);
    if (cached != null) return cached;
    const pending = this.beneficiaryMemberRequestsByGroup.get(groupId);
    if (pending != null) return pending;

    const request = this.groupService.findAllMembers(groupId).then(results => {
      const members = results.map(({ user }) => ({
        ...user,
        label: `${user.firstName} ${user.lastName} (${user.email})`,
      }));
      if (
        this.currentUser != null &&
        !members.some(member => member.id === this.currentUser?.id || member.email.toLowerCase() === this.currentUser?.email.toLowerCase())
      ) {
        members.unshift(this.currentUser);
      }
      this.beneficiaryMembersByGroup.set(groupId, members);
      return members;
    });
    this.beneficiaryMemberRequestsByGroup.set(groupId, request);
    try {
      return await request;
    } finally {
      this.beneficiaryMemberRequestsByGroup.delete(groupId);
    }
  }

  findKnownCurrency(value: string | null | undefined): string | undefined {
    const normalized = normalizeHeader(value ?? '');
    if (normalized === '') return undefined;
    const code = this.currencyOptions.find(candidate => normalizeHeader(candidate) === normalized);
    if (code != null) return code;
    return this.currencies.find(currency => normalizeHeader(currency.name ?? '') === normalized)?.code;
  }

  resolveWalletItemId(value: string | null | undefined): string | undefined {
    const walletItemId = value?.trim().toLowerCase();
    return this.walletItems.find(item => item.id.toLowerCase() === walletItemId)?.id;
  }

  originFor(row: ImportPreviewRow): WalletItemSearchResponseDto | undefined {
    return this.walletItems.find(item => item.id === row.walletItemId);
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

  private loadGroupCategories(groupId: string): Promise<CategoryDto[]> {
    return this.loadEveryCategoryPage(request => this.groupCategoriesService.getAllCategories(groupId, {}, request));
  }

  private async loadEveryCategoryPage(loader: (request: PageRequest) => Promise<Page<CategoryDto>>): Promise<CategoryDto[]> {
    const pageSize = 500;
    const categories: CategoryDto[] = [];
    let pageNumber = 0;
    while (true) {
      const page = await loader({ size: pageSize, page: pageNumber, sort: 'name' });
      categories.push(...page.content);
      if (page.last === true || page.content.length < pageSize) break;
      pageNumber += 1;
    }
    return this.flattenCategories(categories);
  }

  private flattenCategories(categories: CategoryDto[]): CategoryDto[] {
    return categories.flatMap(category => [category, ...this.flattenCategories(category.children ?? [])]);
  }
}
