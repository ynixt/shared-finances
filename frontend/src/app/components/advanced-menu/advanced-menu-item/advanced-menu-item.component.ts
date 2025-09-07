import { NgTemplateOutlet } from '@angular/common';
import { Component, Input, ViewEncapsulation } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faAngleDown, faAngleUp } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import { filter } from 'rxjs';

import { PrimeTemplate } from 'primeng/api';
import { Button, ButtonDirective } from 'primeng/button';
import { Panel } from 'primeng/panel';
import { Skeleton } from 'primeng/skeleton';
import { Tooltip } from 'primeng/tooltip';

import { AdvancedMenuItem } from '../advanced-menu.component';

@Component({
  selector: 'app-advanced-menu-item',
  imports: [
    NgTemplateOutlet,
    FaIconComponent,
    RouterLinkActive,
    RouterLink,
    Panel,
    PrimeTemplate,
    Button,
    Tooltip,
    ButtonDirective,
    Skeleton,
  ],
  templateUrl: './advanced-menu-item.component.html',
  styleUrl: './advanced-menu-item.component.scss',
  encapsulation: ViewEncapsulation.None,
})
@UntilDestroy()
export class AdvancedMenuItemComponent {
  @Input() item!: AdvancedMenuItem;
  @Input() root = false;

  collapsedIcon = faAngleDown;
  expandedIcon = faAngleUp;
  currentRoute: string;

  constructor(private router: Router) {
    this.currentRoute = this.router.url;

    this.router.events
      .pipe(
        untilDestroyed(this),
        filter(event => event instanceof NavigationEnd),
      )
      .subscribe((event: NavigationEnd) => {
        this.currentRoute = event.urlAfterRedirects;
      });
  }
}
