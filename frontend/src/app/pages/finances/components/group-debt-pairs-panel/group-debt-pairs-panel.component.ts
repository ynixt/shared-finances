import { Component, input } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';

import {
  GroupOverviewDebtPairDto,
  OverviewDashboardDetailDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { LocalCurrencyPipe } from '../../../../pipes/local-currency.pipe';

@Component({
  selector: 'app-group-debt-pairs-panel',
  imports: [TranslatePipe, LocalCurrencyPipe, ConfirmDialog, ButtonDirective],
  templateUrl: './group-debt-pairs-panel.component.html',
  providers: [ConfirmationService],
})
export class GroupDebtPairsPanelComponent {
  readonly debtPairs = input<GroupOverviewDebtPairDto[]>([]);
  readonly currency = input<string | undefined>(undefined);

  selectedPair: GroupOverviewDebtPairDto | undefined;

  constructor(
    private readonly confirmationService: ConfirmationService,
    private readonly translateService: TranslateService,
  ) {}

  showDetails(pair: GroupOverviewDebtPairDto) {
    this.selectedPair = pair;
    this.confirmationService.confirm({
      header: this.translateService.instant('financesPage.groupsPage.overviewPage.debtPairs.detailTitle', {
        payer: pair.payerName,
        receiver: pair.receiverName,
      }),
      message: `${pair.payerName} -> ${pair.receiverName}`,
      closable: true,
      closeOnEscape: true,
      rejectVisible: false,
      acceptLabel: this.translateService.instant('financesPage.overviewPage.actions.close'),
      acceptButtonProps: {
        severity: 'secondary',
      },
      accept: () => {
        this.selectedPair = undefined;
      },
    });
  }

  trackDebtPair(index: number, pair: GroupOverviewDebtPairDto): string {
    return `${pair.payerId}-${pair.receiverId}-${pair.currency}-${index}`;
  }

  trackDetail(index: number, detail: OverviewDashboardDetailDto): string {
    return `${index}-${detail.sourceType}-${detail.sourceId ?? ''}-${detail.label}`;
  }
}
