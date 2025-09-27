import { Component, ViewEncapsulation } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { faBuildingColumns, faCreditCard, faGrip, faHashtag, faPlus, faTag } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslateService } from '@ngx-translate/core';

import { startWith } from 'rxjs';

import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

import { AdvancedMenuComponent, AdvancedMenuItem } from '../../../components/advanced-menu/advanced-menu.component';
import { NavbarComponent } from '../../../components/navbar/navbar.component';
import { GroupDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { UserService } from '../../../services/user.service';
import { GroupService } from '../services/group.service';
import { GroupsActionEventService } from '../services/groups-action-event.service';
import { UserActionEventService } from '../services/user-action-event.service';

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

  private groups: GroupDto[] | undefined = undefined;
  private groupMenuRoot: AdvancedMenuItem | undefined;

  constructor(
    private translateService: TranslateService,
    private userService: UserService,
    private router: Router,
    private groupService: GroupService,
    private userActionEventService: UserActionEventService,
    groupsActionEventService: GroupsActionEventService,
  ) {
    groupsActionEventService.groupDeleted$.pipe(untilDestroyed(this)).subscribe(event => {
      const groupId = event.data;

      const index = this.groups?.findIndex(g => g.id == groupId);

      if (index != null && index !== -1) {
        this.groups?.splice(index, 1);
      }

      this.convertGroupsIntoMenu();
    });

    groupsActionEventService.groupUpdated$.pipe(untilDestroyed(this)).subscribe(event => {
      const group = event.data;

      const index = this.groups?.findIndex(g => g.id == group.id);

      if (this.groups != null && index != null && index !== -1) {
        this.groups[index] = group;
      }

      this.convertGroupsIntoMenu();
    });

    this.userService.getUser().then(u => {
      if (u && u.defaultCurrency == null) {
        this.router.navigate(['/welcome'], {
          queryParams: {
            return_to: this.router.url,
          },
        });
      }
    });

    this.translateService.onLangChange.pipe(startWith(this.translateService.currentLang), untilDestroyed(this)).subscribe(() => {
      this.loadItems();
    });

    this.loadGroups().then(() => {
      this.menuItemsNestedChange();
    });

    this.userActionEventService.groupInserted$.pipe(untilDestroyed(this)).subscribe(newGroup => {
      if (this.groups == null) return;

      this.groups!!.push(newGroup);
      this.groups!! = this.groups!!.sort((a, b) => a.name.localeCompare(b.name));
      this.convertGroupsIntoMenu();
      this.menuItemsNestedChange();
    });
  }

  private menuItemsNestedChange() {
    if (this.items != null) {
      this.items = [...this.items];
    }
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
        ],
      },
    ];

    this.groupMenuRoot = {
      expanded: true,
      label: this.translateService.instant('financesPage.menu.groups'),
      itemsMaxHeight: '510px',
      rightButtons: [
        {
          fa: faPlus,
          tooltip: this.translateService.instant('general.new'),
          routerLink: '/app/groups/new',
        },
      ],
      itemsLoading: this.groups == undefined,
      messageIfEmptyItems: this.translateService.instant('financesPage.groupsPage.noGroupYet'),
    };

    this.items.push(this.groupMenuRoot);
    this.convertGroupsIntoMenu();
  }

  private async loadGroups() {
    this.groups = await this.groupService.getAllGroups();
    this.convertGroupsIntoMenu();
  }

  private convertGroupsIntoMenu() {
    if (!this.groupMenuRoot || !this.groups) return;

    const items: AdvancedMenuItem[] = this.groups.map(g => ({
      id: 'group-' + g.id,
      label: g.name,
      expanded: false,
      routeToAutoExpand: ['/app/groups', g.id].join('/'),
      showCollapseIcon: true,
      items: [
        {
          fa: faGrip,
          label: this.translateService.instant('financesPage.menu.overview'),
          routerLink: ['/app/groups', g.id],
          routerLinkActiveOptions: { exact: true },
        },
        {
          label: this.translateService.instant('financesPage.menu.links'),
          expanded: true,
          items: [
            {
              fa: faBuildingColumns,
              label: this.translateService.instant('financesPage.menu.bankAccounts'),
              routerLink: ['/app/groups', g.id, 'bankAccounts'],
              routerLinkActiveOptions: { exact: true },
            },
            {
              fa: faCreditCard,
              label: this.translateService.instant('financesPage.menu.creditCards'),
              routerLink: ['/app/groups', g.id, 'creditCards'],
              routerLinkActiveOptions: { exact: true },
            },
            {
              fa: faTag,
              label: this.translateService.instant('financesPage.menu.categories'),
              routerLink: ['/app/groups', g.id, 'categories'],
              routerLinkActiveOptions: { exact: true },
            },
          ],
        },
      ],
    }));

    this.groupMenuRoot!!.itemsLoading = false;
    this.groupMenuRoot!!.items = items;
  }
}
