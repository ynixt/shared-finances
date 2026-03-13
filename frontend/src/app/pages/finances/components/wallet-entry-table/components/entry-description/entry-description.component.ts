import { Component, computed, input } from '@angular/core';
import { IconDefinition } from '@fortawesome/angular-fontawesome';
import { faHashtag, faTag } from '@fortawesome/free-solid-svg-icons';
import { faBuildingColumns, faCreditCard } from '@fortawesome/free-solid-svg-icons';

import { WalletItemForEntryListDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { EventForListDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
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
  readonly event = input<EventForListDto | undefined>(undefined);
  readonly inGroup = input<boolean>(false);

  readonly originIcon = computed(() => {
    const event = this.event();

    if (event == null) return undefined;

    return this.getIconForWalletItem(event.entries[0].walletItem);
  });

  readonly targetIcon = computed(() => {
    const event = this.event();

    if (event == null || event.entries.length < 2) return undefined;

    return this.getIconForWalletItem(event.entries[1].walletItem);
  });

  readonly originLink = computed(() => {
    const event = this.event();

    if (event == null) return undefined;

    return this.getLinkForWalletItem(event.entries[0].walletItem, this.inGroup());
  });

  readonly targetLink = computed(() => {
    const event = this.event();

    if (event == null || event.entries.length < 2) return undefined;

    return this.getLinkForWalletItem(event.entries[1].walletItem, this.inGroup());
  });

  readonly categoryLink = computed(() => {
    const event = this.event();
    const inGroup = this.inGroup();

    if (event == null || !event.category) return undefined;

    return inGroup ? 'TODO' : `/app/transactions?category=${event.category.id}`;
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
