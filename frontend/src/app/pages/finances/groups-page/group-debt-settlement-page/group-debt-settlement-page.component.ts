import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { faBuildingColumns, faCreditCard } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { ProgressSpinner } from 'primeng/progressspinner';
import { Select } from 'primeng/select';

import { DatePickerComponent } from '../../../../components/date-picker/date-picker.component';
import { GroupUserDto, GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { UserResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { UserService } from '../../../../services/user.service';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import {
  WalletItemPickerComponent,
  WalletItemSearchResponseDtoWithIcon,
} from '../../components/item-picker/wallet-item-picker/wallet-item-picker.component';
import { GroupWalletItemService } from '../../services/group-wallet-item.service';
import { GroupService } from '../../services/group.service';
import { WalletEntryService } from '../../services/wallet-entry.service';
import { buildDebtSettlementEntry } from '../group-debts-page/group-debts-page.helpers';

interface MemberOption {
  label: string;
  userId: string;
}

@Component({
  selector: 'app-group-debt-settlement-page',
  imports: [
    ButtonDirective,
    DatePickerComponent,
    FinancesTitleBarComponent,
    InputNumber,
    ProgressSpinner,
    ReactiveFormsModule,
    Select,
    TranslatePipe,
    WalletItemPickerComponent,
  ],
  templateUrl: './group-debt-settlement-page.component.html',
})
export class GroupDebtSettlementPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly groupService = inject(GroupService);
  private readonly groupWalletItemService = inject(GroupWalletItemService);
  private readonly walletEntryService = inject(WalletEntryService);
  private readonly messageService = inject(MessageService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly translateService = inject(TranslateService);
  private readonly userService = inject(UserService);

  readonly groupId = this.route.snapshot.paramMap.get('id') ?? '';

  readonly group = signal<GroupWithRoleDto | undefined>(undefined);
  readonly members = signal<GroupUserDto[]>([]);
  readonly bankAccounts = signal<WalletItemSearchResponseDto[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly canMutate = computed(() => this.group()?.permissions?.includes('SEND_ENTRIES') === true);

  readonly memberOptions = computed<MemberOption[]>(() =>
    this.members().map(member => ({
      userId: member.user.id,
      label: `${member.user.firstName} ${member.user.lastName}`.trim(),
    })),
  );

  currentUser: UserResponseDto | undefined | null;

  readonly settlementForm = this.formBuilder.group({
    payerId: [undefined as string | undefined, [Validators.required]],
    receiverId: [undefined as string | undefined, [Validators.required]],
    origin: [undefined as WalletItemSearchResponseDtoWithIcon | undefined, [Validators.required]],
    target: [undefined as WalletItemSearchResponseDtoWithIcon | undefined, [Validators.required]],
    amount: [undefined as number | undefined, [Validators.required, Validators.min(0.01)]],
    date: [new Date(), [Validators.required]],
  });

  constructor() {
    this.userService.getUser().then(u => (this.currentUser = u));

    this.settlementForm.get('payerId')?.valueChanges.subscribe(() => {
      this.settlementForm.patchValue({ origin: undefined }, { emitEvent: false });
    });
    this.settlementForm.get('receiverId')?.valueChanges.subscribe(() => {
      this.settlementForm.patchValue({ target: undefined }, { emitEvent: false });
    });

    if (this.groupId) {
      this.patchPrefillFromQueryParams();
      void this.load();
    } else {
      this.loading.set(false);
    }
  }

  amountCurrency(): string {
    return this.settlementForm.get('origin')?.value?.currency ?? this.currentUser?.defaultCurrency ?? 'USD';
  }

  async loadOriginWalletOptions(page = 0, query?: string): Promise<WalletItemSearchResponseDtoWithIcon[]> {
    return this.filterWalletOptionsForUser(this.settlementForm.get('payerId')?.value, query);
  }

  async loadTargetWalletOptions(page = 0, query?: string): Promise<WalletItemSearchResponseDtoWithIcon[]> {
    return this.filterWalletOptionsForUser(this.settlementForm.get('receiverId')?.value, query);
  }

  async submit() {
    if (this.settlementForm.invalid || this.saving() || !this.canMutate()) {
      return;
    }

    this.saving.set(true);
    const raw = this.settlementForm.getRawValue();

    try {
      await this.walletEntryService.createWalletEntry(
        buildDebtSettlementEntry({
          amount: raw.amount!,
          date: raw.date!,
          groupId: this.groupId,
          name: this.translateService.instant('financesPage.groupsPage.debtsPage.settlementEntryName'),
          originId: raw.origin!.id,
          targetId: raw.target!.id,
        }),
      );

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.debtsPage.settlementSaved'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await this.router.navigate(['/app/groups', this.groupId, 'debts']);
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
    } finally {
      this.saving.set(false);
    }
  }

  private patchPrefillFromQueryParams() {
    const payerId = this.route.snapshot.queryParamMap.get('payerId') ?? undefined;
    const receiverId = this.route.snapshot.queryParamMap.get('receiverId') ?? undefined;
    const amountRaw = Number(this.route.snapshot.queryParamMap.get('amount'));
    const amount = Number.isFinite(amountRaw) && amountRaw > 0 ? amountRaw : undefined;

    this.settlementForm.patchValue({
      payerId,
      receiverId,
      amount,
    });
  }

  private async load() {
    this.loading.set(true);

    try {
      const [group, members, bankAccountsPage] = await Promise.all([
        this.groupService.getGroup(this.groupId),
        this.groupService.findAllMembers(this.groupId),
        this.groupWalletItemService.getAllItems(this.groupId, { page: 0, size: 200 }, true),
      ]);

      this.group.set(group);
      this.members.set(members);
      this.bankAccounts.set(bankAccountsPage.content);
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
    } finally {
      this.loading.set(false);
    }
  }

  private filterWalletOptionsForUser(userId?: string | null, query?: string): WalletItemSearchResponseDtoWithIcon[] {
    if (!userId) {
      return [];
    }

    const normalizedQuery = query?.trim().toLowerCase();

    return this.bankAccounts()
      .filter(item => item.user?.id === userId)
      .filter(item => {
        if (!normalizedQuery) {
          return true;
        }

        const searchText = `${item.name} ${item.currency} ${item.type}`.toLowerCase();
        return searchText.includes(normalizedQuery);
      })
      .map(item => ({
        ...item,
        icon: item.type === 'CREDIT_CARD' ? faCreditCard : faBuildingColumns,
      }));
  }
}
