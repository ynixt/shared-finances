import { Component, EventEmitter, Input, Output } from '@angular/core';
import { IconDefinition } from '@fortawesome/angular-fontawesome';

import { MenuItem } from 'primeng/api';

import { AdvancedMenuItemComponent } from './advanced-menu-item/advanced-menu-item.component';

export type AdvancedMenuItem = {
  fa?: IconDefinition;
  showCollapseIcon?: boolean;
  routeToAutoExpand?: string;
  rightButtons?: {
    tooltip?: string;
    fa?: IconDefinition;
    routerLink?: string | string[];
    command?: () => void;
  }[];
  items?: AdvancedMenuItem & MenuItem[];
  itemsLoading?: boolean;
  messageIfEmptyItems?: string;
  itemsMaxHeight?: string;
} & MenuItem;

@Component({
  selector: 'app-advanced-menu',
  imports: [AdvancedMenuItemComponent],
  templateUrl: './advanced-menu.component.html',
  styleUrl: './advanced-menu.component.scss',
})
export class AdvancedMenuComponent {
  @Input() items: AdvancedMenuItem[] | undefined;
  @Input() drawerOpen = false;
  @Output() drawerOpenChange = new EventEmitter();

  itemClicked(item: AdvancedMenuItem) {
    this.drawerOpen = false;
    this.drawerOpenChange.emit(this.drawerOpen);
  }
}
