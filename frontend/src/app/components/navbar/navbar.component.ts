import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MenuItem, MenuItemCommandEvent } from 'primeng/api';
import { ButtonDirective, ButtonLabel } from 'primeng/button';
import { Menu } from 'primeng/menu';
import { ProgressSpinner } from 'primeng/progressspinner';

import { KratosAuthService } from '../../services/kratos-auth.service';
import { UserService } from '../../services/user.service';
import { LangButtonComponent } from '../lang-button/lang-button.component';

@Component({
  selector: 'app-navbar',
  imports: [ButtonDirective, TranslatePipe, ButtonLabel, RouterLink, LangButtonComponent, ProgressSpinner, Menu],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {
  items: MenuItem[] | undefined;

  constructor(
    public userService: UserService,
    private translateService: TranslateService,
    private kratosAuthService: KratosAuthService,
  ) {
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

  async logout() {
    return this.kratosAuthService.logout();
  }
}
