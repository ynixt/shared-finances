import { Component, computed, input } from '@angular/core';
import { FaIconComponent, IconDefinition } from '@fortawesome/angular-fontawesome';
import { faBuildingColumns, faCreditCard, faUser } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

import { WalletItemSearchResponseDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';

export type WalletItemOption = Pick<WalletItemSearchResponseDto, 'id' | 'name'> &
  Partial<Omit<WalletItemSearchResponseDto, 'id' | 'name'>> & { icon?: IconDefinition };

@Component({
  selector: 'app-wallet-item-option',
  standalone: true,
  imports: [FaIconComponent, TranslatePipe],
  templateUrl: './wallet-item-option.component.html',
})
export class WalletItemOptionComponent {
  readonly item = input.required<WalletItemOption>();
  readonly mode = input<'option' | 'selected' | 'chip'>('option');

  readonly iconUser = faUser;
  readonly icon = computed(() => {
    const item = this.item();
    if (item.icon != null) return item.icon;
    if (item.type == null) return undefined;
    return item.type === 'CREDIT_CARD' ? faCreditCard : faBuildingColumns;
  });
}
