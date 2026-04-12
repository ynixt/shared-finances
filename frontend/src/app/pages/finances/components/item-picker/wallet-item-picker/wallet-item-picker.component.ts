import { Component, ViewChild, effect, forwardRef, inject, input } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { FaIconComponent, IconDefinition } from '@fortawesome/angular-fontawesome';
import { faBuildingColumns, faCreditCard, faUser } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

import { PagedSelectComponent } from '../../../../../components/paged-select/paged-select.component';
import { SimpleControlValueAccessor } from '../../../../../components/simple-control-value-accessor';
import { GroupDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { WalletItemSearchResponseDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { GroupWalletItemService } from '../../../services/group-wallet-item.service';
import { WalletItemService } from '../../../services/wallet-item.service';

export type WalletItemSearchResponseDtoWithIcon = WalletItemSearchResponseDto & { icon: IconDefinition };

@Component({
  selector: 'app-wallet-item-picker',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, PagedSelectComponent, FaIconComponent, TranslatePipe],
  templateUrl: './wallet-item-picker.component.html',
  styleUrl: './wallet-item-picker.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WalletItemPickerComponent),
      multi: true,
    },
  ],
})
export class WalletItemPickerComponent extends SimpleControlValueAccessor<WalletItemSearchResponseDtoWithIcon> {
  readonly iconUser = faUser;
  private readonly walletItemService = inject(WalletItemService);
  private readonly groupWalletItemService = inject(GroupWalletItemService);

  @ViewChild('pagedSelect') pagedSelect: PagedSelectComponent | undefined = undefined;

  optionsGetter = input<(page: number, query?: string | undefined) => Promise<WalletItemSearchResponseDtoWithIcon[]>>(
    this.loadOrigins.bind(this),
  );
  group = input<GroupDto | undefined>(undefined);
  /** When true, only bank accounts are loaded (server-side filter). */
  onlyBankAccounts = input(false);

  constructor() {
    super();

    effect(() => {
      this.group();
      this.onlyBankAccounts();

      this.pagedSelect?.resetComponent();
    });
  }

  async loadOrigins(page = 0, query: string | undefined): Promise<WalletItemSearchResponseDtoWithIcon[]> {
    const group = this.group();
    const onlyBank = this.onlyBankAccounts();

    const t = await (group == null
      ? this.walletItemService.getAllItems(
          {
            sort: 'name',
            page,
          },
          onlyBank,
        )
      : this.groupWalletItemService.getAllItems(
          group.id,
          {
            sort: 'name',
            page,
          },
          onlyBank,
        ));

    const getIcon = (item: WalletItemSearchResponseDto) => {
      return item.type === 'CREDIT_CARD' ? faCreditCard : faBuildingColumns;
    };

    return t.content.map(item => ({ ...item, icon: getIcon(item) })) as WalletItemSearchResponseDtoWithIcon[];
  }
}
