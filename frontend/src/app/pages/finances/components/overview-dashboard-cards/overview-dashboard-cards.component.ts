import { Component, input } from '@angular/core';
import {
  faArrowRightArrowLeft,
  faArrowTrendDown,
  faArrowTrendUp,
  faBullseye,
  faChartSimple,
  faClock,
  faDollarSign,
  faScaleBalanced,
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
  readonly goalCommittedIcon = faBullseye;
  readonly goalFreeBalanceIcon = faWallet;
  readonly periodCashInIcon = faArrowTrendUp;
  readonly periodCashOutIcon = faArrowTrendDown;
  readonly periodNetCashFlowIcon = faArrowRightArrowLeft;
  readonly projectedCashInIcon = faClock;
  readonly projectedCashOutIcon = faClock;
  readonly endBalanceIcon = faWallet;
  readonly endNetCashFlowIcon = faChartSimple;
  readonly groupMemberDebtsIcon = faScaleBalanced;

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
        GOAL_COMMITTED: 'financesPage.overviewPage.cards.goalCommitted',
        GOAL_FREE_BALANCE: 'financesPage.overviewPage.cards.goalFreeBalance',
        PERIOD_CASH_IN: 'financesPage.overviewPage.cards.periodCashIn',
        PERIOD_CASH_OUT: 'financesPage.overviewPage.cards.periodCashOut',
        PERIOD_NET_CASH_FLOW: 'financesPage.overviewPage.cards.periodNetCashFlow',
        PROJECTED_CASH_IN: 'financesPage.overviewPage.cards.projectedCashIn',
        PROJECTED_CASH_OUT: 'financesPage.overviewPage.cards.projectedCashOut',
        END_OF_PERIOD_BALANCE: 'financesPage.overviewPage.cards.endOfPeriodBalance',
        EXPENSES: 'financesPage.overviewPage.cards.expenses',
        PROJECTED_EXPENSES: 'financesPage.overviewPage.cards.projectedExpenses',
        PERIOD_EXPENSES: 'financesPage.overviewPage.cards.periodExpenses',
        END_OF_PERIOD_NET_CASH_FLOW: 'financesPage.overviewPage.cards.endOfPeriodNetCashFlow',
        GROUP_MEMBER_DEBTS: 'financesPage.overviewPage.cards.groupMemberDebts',
      }[cardKey] ?? 'financesPage.overviewPage.unknownCard'
    );
  }

  cardIcon(cardKey: string) {
    return (
      {
        BALANCE: this.balanceIcon,
        GOAL_COMMITTED: this.goalCommittedIcon,
        GOAL_FREE_BALANCE: this.goalFreeBalanceIcon,
        PERIOD_CASH_IN: this.periodCashInIcon,
        PERIOD_CASH_OUT: this.periodCashOutIcon,
        PERIOD_NET_CASH_FLOW: this.periodNetCashFlowIcon,
        PROJECTED_CASH_IN: this.projectedCashInIcon,
        PROJECTED_CASH_OUT: this.projectedCashOutIcon,
        END_OF_PERIOD_BALANCE: this.endBalanceIcon,
        END_OF_PERIOD_NET_CASH_FLOW: this.endNetCashFlowIcon,
        EXPENSES: this.periodCashOutIcon,
        PROJECTED_EXPENSES: this.periodCashOutIcon,
        PERIOD_EXPENSES: this.periodCashOutIcon,
        GROUP_MEMBER_DEBTS: this.groupMemberDebtsIcon,
      }[cardKey] ?? this.balanceIcon
    );
  }

  cardValueClass(card: OverviewDashboardCardDto): string {
    if (card.key === 'GOAL_COMMITTED') {
      return card.value >= 0 ? 'text-surface-900 dark:text-surface-0' : 'text-red-700 dark:text-red-400';
    }

    const inverseColors = this.inverseColors(card.key);

    if (inverseColors) {
      return card.value <= 0 ? 'text-green-700 dark:text-green-400' : 'text-red-700 dark:text-red-400';
    } else {
      return card.value >= 0 ? 'text-green-700 dark:text-green-400' : 'text-red-700 dark:text-red-400';
    }
  }

  detailLabel(detail: OverviewDashboardDetailDto): string {
    return detail.label;
  }

  detailSublabel(detail: OverviewDashboardDetailDto): string {
    return (
      {
        BANK_ACCOUNT: 'financesPage.overviewPage.detail.sourceType.bankAccount',
        CREDIT_CARD_BILL: 'financesPage.overviewPage.detail.sourceType.creditCardBill',
        GOAL: 'financesPage.overviewPage.detail.sourceType.goal',
        FORMULA: 'financesPage.overviewPage.detail.sourceType.formula',
        GROUP_DEBT_PAIR: 'financesPage.overviewPage.detail.sourceType.groupDebtPair',
      }[detail.sourceType] ?? 'financesPage.overviewPage.detail.sourceType.unknown'
    );
  }

  detailValueCss(card: OverviewDashboardCardDto, detail: OverviewDashboardDetailDto): string {
    const inverseColors = this.inverseColors(card.key);

    if (inverseColors) {
      return detail.value <= 0 ? 'text-green-700 dark:text-green-400' : 'text-red-700 dark:text-red-400';
    } else {
      return detail.value >= 0 ? 'text-green-700 dark:text-green-400' : 'text-red-700 dark:text-red-400';
    }
  }

  trackByCardKey(index: number, card: OverviewDashboardCardDto) {
    return card.key;
  }

  trackDetail(index: number, detail: OverviewDashboardDetailDto): string {
    return `${index}-${detail.sourceType}-${detail.sourceId ?? ''}-${detail.label}`;
  }

  private inverseColors(key: string): boolean {
    return key === 'EXPENSES' || key === 'PROJECTED_EXPENSES' || key === 'PERIOD_EXPENSES' || key === 'PERIOD_CASH_OUT';
  }
}
