import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconDefinition } from '@fortawesome/angular-fontawesome';
import { faHashtag, faTag } from '@fortawesome/free-solid-svg-icons';
import { faBuildingColumns, faCreditCard } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

import { WalletItemForEntryListDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { EventForListDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { WalletItemType__Obj } from '../../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { EntryChipComponent } from './entry-chip/entry-chip.component';

@Component({
  selector: 'app-entry-description',
  imports: [EntryChipComponent, TranslatePipe, RouterLink],
  templateUrl: './entry-description.component.html',
  styleUrl: './entry-description.component.scss',
  standalone: true,
})
export class EntryDescriptionComponent {
  readonly categoryIcon = faTag;
  readonly tagIcon = faHashtag;
  readonly event = input<EventForListDto | undefined>(undefined);
  readonly inGroup = input<boolean>(false);
  readonly showWalletOwner = input<boolean>(false);
  readonly currentUserId = input<string | undefined>(undefined);

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

    return this.getLinkForWalletItem(event.entries[0].walletItem, this.inGroup(), this.currentUserId());
  });

  readonly targetLink = computed(() => {
    const event = this.event();

    if (event == null || event.entries.length < 2) return undefined;

    return this.getLinkForWalletItem(event.entries[1].walletItem, this.inGroup(), this.currentUserId());
  });

  readonly categoryLink = computed(() => {
    return undefined;
  });

  getTagLink = (tagId: string, inGroup: boolean) => {
    return undefined;
  };

  private getIconForWalletItem(walletItem: WalletItemForEntryListDto): IconDefinition {
    if (walletItem.type == WalletItemType__Obj.BANK_ACCOUNT) {
      return faBuildingColumns;
    } else {
      return faCreditCard;
    }
  }

  private getLinkForWalletItem(
    walletItem: WalletItemForEntryListDto,
    inGroup: boolean,
    currentUserId: string | undefined,
  ): string | string[] | undefined {
    if (walletItem.user?.id != currentUserId) return undefined;

    if (walletItem.type == WalletItemType__Obj.BANK_ACCOUNT) {
      return ['/app', 'bankAccounts', walletItem.id];
    } else {
      return ['/app', 'creditCards', walletItem.id];
    }
  }
}
