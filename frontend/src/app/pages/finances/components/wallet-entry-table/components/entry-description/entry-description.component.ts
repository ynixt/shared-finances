import { Component, computed, input } from '@angular/core';
import { IconDefinition } from '@fortawesome/angular-fontawesome';
import { faHashtag, faTag } from '@fortawesome/free-solid-svg-icons';
import { faBuildingColumns, faCreditCard } from '@fortawesome/free-solid-svg-icons';

import { WalletItemForEntryListDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { EntryForListDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { WalletItemType__Obj } from '../../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { EntryChipComponent } from './entry-chip/entry-chip.component';

@Component({
  selector: 'app-entry-description',
  imports: [EntryChipComponent],
  templateUrl: './entry-description.component.html',
  styleUrl: './entry-description.component.scss',
  standalone: true,
})
export class EntryDescriptionComponent {
  readonly categoryIcon = faTag;
  readonly tagIcon = faHashtag;
  readonly entry = input<EntryForListDto | undefined>(undefined);
  readonly inGroup = input<boolean>(false);

  readonly originIcon = computed(() => {
    const entry = this.entry();

    if (entry == null) return undefined;

    return this.getIconForWalletItem(entry.origin);
  });

  readonly targetIcon = computed(() => {
    const entry = this.entry();

    if (entry == null || entry.target == null) return undefined;

    return this.getIconForWalletItem(entry.target);
  });

  readonly originLink = computed(() => {
    const entry = this.entry();

    if (entry == null) return undefined;

    return this.getLinkForWalletItem(entry.origin, this.inGroup());
  });

  readonly targetLink = computed(() => {
    const entry = this.entry();

    if (entry == null || entry.target == null) return undefined;

    return this.getLinkForWalletItem(entry.target, this.inGroup());
  });

  readonly categoryLink = computed(() => {
    const entry = this.entry();
    const inGroup = this.inGroup();

    if (entry == null || !entry.category) return undefined;

    return inGroup ? 'TODO' : `/app/transactions?category=${entry.category.id}`;
  });

  getTagLink = (tagId: string, inGroup: boolean) => {
    return inGroup ? '' : `/app/transactions?tags=${tagId}`;
  };

  private getIconForWalletItem(walletItem: WalletItemForEntryListDto): IconDefinition {
    if (walletItem.type == WalletItemType__Obj.BANK_ACCOUNT) {
      return faBuildingColumns;
    } else {
      return faCreditCard;
    }
  }

  private getLinkForWalletItem(walletItem: WalletItemForEntryListDto, inGroup: boolean): string {
    if (inGroup) {
      throw 'TODO';
    } else {
      if (walletItem.type == WalletItemType__Obj.BANK_ACCOUNT) {
        return `/app/bankAccounts/${walletItem.id}`;
      } else {
        return `/app/creditCards/${walletItem.id}`;
      }
    }
  }
}
