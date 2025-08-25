import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FaIconComponent, IconDefinition } from '@fortawesome/angular-fontawesome';

import { MenuItem, PrimeTemplate } from 'primeng/api';
import { Menu } from 'primeng/menu';

export type FaMenuItem = MenuItem & { fa?: IconDefinition };

@Component({
  selector: 'app-menu-with-fa',
  imports: [Menu, FaIconComponent, PrimeTemplate, RouterLink, RouterLinkActive],
  templateUrl: './menu-with-fa.component.html',
  styleUrl: './menu-with-fa.component.scss',
})
export class MenuWithFaComponent {
  @Input() items: FaMenuItem[] | undefined;
}
