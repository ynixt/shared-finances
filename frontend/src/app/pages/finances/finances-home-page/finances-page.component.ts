import { Component, ViewEncapsulation } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { faBuildingColumns, faCreditCard, faGrip, faHashtag, faPlus, faTag } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslateService } from '@ngx-translate/core';

import { startWith } from 'rxjs';

import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

import { AdvancedMenuComponent, AdvancedMenuItem } from '../../../components/advanced-menu/advanced-menu.component';
import { NavbarComponent } from '../../../components/navbar/navbar.component';

@Component({
  selector: 'app-finances-page',
  imports: [NavbarComponent, RouterOutlet, Toast, AdvancedMenuComponent],
  templateUrl: './finances-page.component.html',
  styleUrl: './finances-page.component.scss',
  encapsulation: ViewEncapsulation.None,
  providers: [MessageService],
})
@UntilDestroy()
export class FinancesPageComponent {
  drawerOpen = false;

  items: AdvancedMenuItem[] | undefined;

  constructor(private translateService: TranslateService) {
    this.translateService.onLangChange.pipe(startWith(this.translateService.currentLang), untilDestroyed(this)).subscribe(() => {
      this.loadItems();
    });
  }

  private loadItems() {
    this.items = [
      {
        fa: faGrip,
        label: this.translateService.instant('financesPage.menu.overview'),
        routerLink: ['/app'],
        routerLinkActiveOptions: { exact: true },
      },
      {
        label: this.translateService.instant('financesPage.menu.registrations'),
        expanded: true,
        items: [
          {
            fa: faBuildingColumns,
            label: this.translateService.instant('financesPage.menu.bankAccounts'),
            routerLink: '/app/bankAccounts',
            routerLinkActiveOptions: { exact: true },
          },
          {
            fa: faCreditCard,
            label: this.translateService.instant('financesPage.menu.creditCards'),
            routerLink: '/app/creditCards',
            routerLinkActiveOptions: { exact: true },
          },
          {
            fa: faTag,
            label: this.translateService.instant('financesPage.menu.categories'),
            routerLink: '/app/categories',
            routerLinkActiveOptions: { exact: true },
          },
          {
            fa: faHashtag,
            label: this.translateService.instant('financesPage.menu.tags'),
            routerLink: '/app/tags',
            routerLinkActiveOptions: { exact: true },
          },
        ],
      },
    ];

    const groups = [
      {
        id: 'asd',
        name: 'Grupo 1',
      },
      {
        id: 'asd2',
        name: 'Grupo 2',
      },
    ]; // TODO

    this.items.push({
      expanded: true,
      label: this.translateService.instant('financesPage.menu.groups'),
      rightButtons: [
        {
          fa: faPlus,
          tooltip: this.translateService.instant('general.new'),
          routerLink: '/app/groups/new',
        },
      ],
      items: groups.map(g => ({
        label: g.name,
        expanded: false,
        routeToAutoExpand: ['/app/group', g.id],
        showCollapseIcon: true,
        items: [
          {
            fa: faGrip,
            label: this.translateService.instant('financesPage.menu.overview'),
            routerLink: ['/app/group', g.id],
            routerLinkActiveOptions: { exact: true },
          },
          {
            label: this.translateService.instant('financesPage.menu.links'),
            expanded: true,
            items: [
              {
                fa: faBuildingColumns,
                label: this.translateService.instant('financesPage.menu.bankAccounts'),
                routerLink: ['/app/group', g.id, 'bankAccounts'],
                routerLinkActiveOptions: { exact: true },
              },
              {
                fa: faCreditCard,
                label: this.translateService.instant('financesPage.menu.creditCards'),
                routerLink: ['/app/group', g.id, 'creditCards'],
                routerLinkActiveOptions: { exact: true },
              },
              {
                fa: faTag,
                label: this.translateService.instant('financesPage.menu.categories'),
                routerLink: ['/app/group', g.id, 'categories'],
                routerLinkActiveOptions: { exact: true },
              },
              {
                fa: faHashtag,
                label: this.translateService.instant('financesPage.menu.tags'),
                routerLink: ['/app/group', g.id, 'tags'],
                routerLinkActiveOptions: { exact: true },
              },
            ],
          },
        ],
      })),
    });
  }
}
