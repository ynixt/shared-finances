import { Component, ViewEncapsulation } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { faBuildingColumns, faCreditCard, faGrip, faHashtag, faTag } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslateService } from '@ngx-translate/core';

import { startWith } from 'rxjs';

import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

import { FaMenuItem, MenuWithFaComponent } from '../../../components/menu-with-fa/menu-with-fa.component';
import { NavbarComponent } from '../../../components/navbar/navbar.component';

@Component({
  selector: 'app-finances-page',
  imports: [NavbarComponent, MenuWithFaComponent, RouterOutlet, Toast],
  templateUrl: './finances-page.component.html',
  styleUrl: './finances-page.component.scss',
  encapsulation: ViewEncapsulation.None,
  providers: [MessageService],
})
@UntilDestroy()
export class FinancesPageComponent {
  items: FaMenuItem[] | undefined;

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
    ];
  }
}
