import { Component, ViewEncapsulation } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { faBuildingColumns, faCreditCard, faGrip, faHashtag, faTag } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslateService } from '@ngx-translate/core';

import { Menu } from 'primeng/menu';

import { FaMenuItem, MenuWithFaComponent } from '../../../components/menu-with-fa/menu-with-fa.component';
import { NavbarComponent } from '../../../components/navbar/navbar.component';

@Component({
  selector: 'app-finances-home-page',
  imports: [NavbarComponent, Menu, MenuWithFaComponent, RouterOutlet],
  templateUrl: './finances-home-page.component.html',
  styleUrl: './finances-home-page.component.scss',
  encapsulation: ViewEncapsulation.None,
})
@UntilDestroy()
export class FinancesHomePageComponent {
  items: FaMenuItem[] | undefined;

  constructor(private translateService: TranslateService) {
    this.translateService.onLangChange.pipe(untilDestroyed(this)).subscribe(lang => {
      this.loadItems();
    });
  }

  private loadItems() {
    this.items = [
      {
        fa: faGrip,
        label: this.translateService.instant('finances.menu.overview'),
        routerLink: ['/app'],
        routerLinkActiveOptions: { exact: true },
      },
      {
        fa: faBuildingColumns,
        label: this.translateService.instant('finances.menu.bankAccounts'),
        routerLink: '/app/bankAccounts',
        routerLinkActiveOptions: { exact: true },
      },
      {
        fa: faCreditCard,
        label: this.translateService.instant('finances.menu.creditCards'),
        routerLink: '/app/creditCards',
        routerLinkActiveOptions: { exact: true },
      },
      {
        fa: faTag,
        label: this.translateService.instant('finances.menu.categories'),
        routerLink: '/app/categories',
        routerLinkActiveOptions: { exact: true },
      },
      {
        fa: faHashtag,
        label: this.translateService.instant('finances.menu.tags'),
        routerLink: '/app/tags',
        routerLinkActiveOptions: { exact: true },
      },
    ];
  }
}
