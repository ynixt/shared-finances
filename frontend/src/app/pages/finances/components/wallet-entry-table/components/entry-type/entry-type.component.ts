import { Component, computed, inject, input } from '@angular/core';
import { FaIconComponent, IconDefinition } from '@fortawesome/angular-fontawesome';
import { faArrowRightArrowLeft, faArrowTrendDown, faArrowTrendUp } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { EventForListDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { WalletEntryType, WalletEntryType__Obj } from '../../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { LangService } from '../../../../../../services/lang.service';

@Component({
  selector: 'app-entry-type',
  imports: [FaIconComponent, TranslatePipe],
  templateUrl: './entry-type.component.html',
  styleUrl: './entry-type.component.scss',
  standalone: true,
})
export class EntryTypeComponent {
  readonly translateService = inject(TranslateService);
  readonly langService = inject(LangService);
  readonly entry = input<EventForListDto | undefined>(undefined);

  type = computed<WalletEntryType | undefined>(() => {
    const entry = this.entry();

    return entry?.type;
  });

  typeFaIcon = computed<IconDefinition | undefined>(() => {
    switch (this.type()) {
      case WalletEntryType__Obj.REVENUE:
        return faArrowTrendUp;
      case WalletEntryType__Obj.EXPENSE:
        return faArrowTrendDown;
      case WalletEntryType__Obj.TRANSFER:
        return faArrowRightArrowLeft;
    }

    return undefined;
  });

  typeText = computed<string | undefined>(() => {
    const type = this.type();

    if (type == undefined) return undefined;

    return `enums.walletEntryType.${type}`;
  });
}
