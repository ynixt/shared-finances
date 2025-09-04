import { NgTemplateOutlet } from '@angular/common';
import { Component, Input, ViewEncapsulation } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faAngleDown, faAngleUp } from '@fortawesome/free-solid-svg-icons';

import { PrimeTemplate } from 'primeng/api';
import { Button, ButtonDirective } from 'primeng/button';
import { Panel } from 'primeng/panel';
import { Tooltip } from 'primeng/tooltip';

import { AdvancedMenuItem } from '../advanced-menu.component';

@Component({
  selector: 'app-advanced-menu-item',
  imports: [NgTemplateOutlet, FaIconComponent, RouterLinkActive, RouterLink, Panel, PrimeTemplate, Button, Tooltip, ButtonDirective],
  templateUrl: './advanced-menu-item.component.html',
  styleUrl: './advanced-menu-item.component.scss',
  encapsulation: ViewEncapsulation.None,
})
export class AdvancedMenuItemComponent {
  @Input() item!: AdvancedMenuItem;
  @Input() root = false;

  collapsedIcon = faAngleDown;
  expandedIcon = faAngleUp;
}
