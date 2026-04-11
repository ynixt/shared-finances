import { Component, input } from '@angular/core';
import {
  faArrowRightArrowLeft,
  faArrowTrendDown,
  faArrowTrendUp,
  faChartSimple,
  faClock,
  faDollarSign,
  faWallet,
} from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialog } from 'primeng/confirmdialog';

import {
  OverviewDashboardCardDto,
  OverviewDashboardDetailDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { LocalCurrencyPipe } from '../../../../pipes/local-currency.pipe';
import { DashboardCardComponent } from '../dashboard-card/dashboard-card.component';

@Component({
  selector: 'app-overview-dashboard-cards',
  imports: [TranslatePipe, LocalCurrencyPipe, DashboardCardComponent, ConfirmDialog],
  templateUrl: './overview-dashboard-cards.component.html',
  providers: [ConfirmationService],
})
export class OverviewDashboardCardsComponent {
  readonly cards = input<OverviewDashboardCardDto[]>([]);
  readonly currency = input<string | undefined>(undefined);

  readonly balanceIcon = faDollarSign;
  readonly periodCashInIcon = faArrowTrendUp;
  readonly periodCashOutIcon = faArrowTrendDown;
  readonly periodNetCashFlowIcon = faArrowRightArrowLeft;
  readonly projectedCashInIcon = faClock;
  readonly projectedCashOutIcon = faClock;
  readonly endBalanceIcon = faWallet;
  readonly endNetCashFlowIcon = faChartSimple;

  selectedCard: OverviewDashboardCardDto | undefined = undefined;

  constructor(
    private readonly confirmationService: ConfirmationService,
    private readonly translateService: TranslateService,
  ) {}

  showDetails(card: OverviewDashboardCardDto) {
    this.selectedCard = card;
    this.confirmationService.confirm({
      header: this.translateService.instant(this.cardTitleKey(card.key)),
      message: card.key,
      closable: true,
      closeOnEscape: true,
      rejectVisible: false,
      acceptLabel: this.translateService.instant('financesPage.overviewPage.actions.close'),
      acceptButtonProps: {
        severity: 'secondary',
      },
      accept: () => {
        this.selectedCard = undefined;
      },
    });
  }

  cardTitleKey(cardKey: string): string {
    return (
      {
        BALANCE: 'financesPage.overviewPage.cards.balance',
        PERIOD_CASH_IN: 'financesPage.overviewPage.cards.periodCashIn',
        PERIOD_CASH_OUT: 'financesPage.overviewPage.cards.periodCashOut',
        PERIOD_NET_CASH_FLOW: 'financesPage.overviewPage.cards.periodNetCashFlow',
        PROJECTED_CASH_IN: 'financesPage.overviewPage.cards.projectedCashIn',
        PROJECTED_CASH_OUT: 'financesPage.overviewPage.cards.projectedCashOut',
        END_OF_PERIOD_BALANCE: 'financesPage.overviewPage.cards.endOfPeriodBalance',
        END_OF_PERIOD_NET_CASH_FLOW: 'financesPage.overviewPage.cards.endOfPeriodNetCashFlow',
      }[cardKey] ?? 'financesPage.overviewPage.unknownCard'
    );
  }

  cardIcon(cardKey: string) {
    return (
      {
        BALANCE: this.balanceIcon,
        PERIOD_CASH_IN: this.periodCashInIcon,
        PERIOD_CASH_OUT: this.periodCashOutIcon,
        PERIOD_NET_CASH_FLOW: this.periodNetCashFlowIcon,
        PROJECTED_CASH_IN: this.projectedCashInIcon,
        PROJECTED_CASH_OUT: this.projectedCashOutIcon,
        END_OF_PERIOD_BALANCE: this.endBalanceIcon,
        END_OF_PERIOD_NET_CASH_FLOW: this.endNetCashFlowIcon,
      }[cardKey] ?? this.balanceIcon
    );
  }

  cardValueClass(card: OverviewDashboardCardDto): string {
    if (card.key === 'PERIOD_CASH_OUT' || card.key === 'PROJECTED_CASH_OUT') {
      return 'text-red-700';
    }

    if (card.key === 'PERIOD_CASH_IN' || card.key === 'PROJECTED_CASH_IN') {
      return 'text-green-700';
    }

    return card.value >= 0 ? 'text-green-700' : 'text-red-700';
  }

  detailLabel(detail: OverviewDashboardDetailDto): string {
    return detail.label;
  }

  detailSublabel(detail: OverviewDashboardDetailDto): string {
    return (
      {
        BANK_ACCOUNT: 'financesPage.overviewPage.detail.sourceType.bankAccount',
        CREDIT_CARD_BILL: 'financesPage.overviewPage.detail.sourceType.creditCardBill',
        FORMULA: 'financesPage.overviewPage.detail.sourceType.formula',
      }[detail.sourceType] ?? 'financesPage.overviewPage.detail.sourceType.unknown'
    );
  }

  trackByCardKey(index: number, card: OverviewDashboardCardDto) {
    return card.key;
  }
}
