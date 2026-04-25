import { Component, EventEmitter, Input, Output, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent, IconDefinition } from '@fortawesome/angular-fontawesome';
import { faChevronLeft, faPlus, faTrash } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';

export type FinancesTitleBarExtraButton = {
  click?: () => void;
  routerLink?: string | string[];
  rounded?: boolean;
  tooltip?: string;
  tooltipPosition?: string;
  loading?: boolean;
  icon?: IconDefinition;
};

@Component({
  selector: 'app-finances-title-bar',
  imports: [FaIconComponent, RouterLink, ButtonDirective, TranslatePipe, Tooltip],
  templateUrl: './finances-title-bar.component.html',
  styleUrl: './finances-title-bar.component.scss',
})
export class FinancesTitleBarComponent {
  protected readonly newButtonIcon = faPlus;
  protected readonly closeButtonIcon = faChevronLeft;
  protected readonly deleteButtonIcon = faTrash;

  @Input() title!: string;

  @Input() closeRouterLink: string | undefined | string[] = undefined;
  @Input() closeClick: (() => any) | undefined = undefined;

  @Output() deleteButtonClicked = new EventEmitter<void>();

  newRouterLink = input<string | string[] | undefined>(undefined);
  extraButtons = input<FinancesTitleBarExtraButton[] | undefined>();
  showDeleteButton = input<boolean>(false);
  deleteButtonLoading = input<boolean>(false);

  rightButtons = computed<FinancesTitleBarExtraButton[]>(() => {
    const base = this.extraButtons() ?? [];
    const list = [...base];

    if (this.newRouterLink()) {
      list.push({
        routerLink: this.newRouterLink(),
        rounded: true,
        tooltip: 'general.new',
        icon: this.newButtonIcon,
      });
    }

    if (this.showDeleteButton()) {
      list.push({
        click: () => this.deleteButtonClicked.next(),
        rounded: true,
        tooltip: 'general.delete',
        tooltipPosition: 'bottom',
        loading: this.deleteButtonLoading(),
        icon: this.deleteButtonIcon,
      });
    }
    return list;
  });

  constructor() {}
}
