import { Component, EventEmitter, Input, Output, effect } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faBars, faBarsStaggered } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { startWith } from 'rxjs';

import { MenuItem, MenuItemCommandEvent } from 'primeng/api';
import { ButtonDirective, ButtonLabel } from 'primeng/button';
import { Divider } from 'primeng/divider';
import { Menu } from 'primeng/menu';
import { ProgressSpinner } from 'primeng/progressspinner';

import { AuthService } from '../../services/auth.service';
import { BreakpointService } from '../../services/breakpoint.service';
import { UserService } from '../../services/user.service';
import { LangButtonComponent } from '../lang-button/lang-button.component';
import { UserAvatarComponent } from '../user-avatar/user-avatar.component';

@Component({
  selector: 'app-navbar',
  imports: [
    ButtonDirective,
    TranslatePipe,
    ButtonLabel,
    RouterLink,
    LangButtonComponent,
    ProgressSpinner,
    Menu,
    FaIconComponent,
    UserAvatarComponent,
    Divider,
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
@UntilDestroy()
export class NavbarComponent {
  @Input() showDrawerMenu = false;
  @Input() drawerOpen = false;
  @Output() drawerOpenChange = new EventEmitter();

  items: MenuItem[] | undefined;

  constructor(
    public userService: UserService,
    private translateService: TranslateService,
    private authService: AuthService,
    breakpointService: BreakpointService,
  ) {
    this.translateService.onLangChange.pipe(startWith(this.translateService.currentLang), untilDestroyed(this)).subscribe(lang => {
      this.loadItems();
    });

    effect(() => {
      if (breakpointService.isUp('lg')()) {
        this.setDrawer(false);
      }
    });
  }

  async logout() {
    return this.authService.logout();
  }

  toggleDrawer() {
    this.setDrawer(!this.drawerOpen);
  }

  setDrawer(open: boolean) {
    this.drawerOpen = open;
    this.drawerOpenChange.emit(this.drawerOpen);
  }

  private loadItems() {
    this.items = [
      {
        label: this.translateService.instant('navbar.logout'),
        icon: 'pi pi-sign-out',
        command: (event: MenuItemCommandEvent) => {
          this.logout();
        },
      },
    ];
  }

  protected readonly faBars = faBars;
  protected readonly faBarsStaggered = faBarsStaggered;
}
