import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective, ButtonLabel } from 'primeng/button';
import { ProgressSpinner } from 'primeng/progressspinner';

import { UserService } from '../../services/user.service';
import { LangButtonComponent } from '../lang-button/lang-button.component';

@Component({
  selector: 'app-navbar',
  imports: [ButtonDirective, TranslatePipe, ButtonLabel, RouterLink, LangButtonComponent, ProgressSpinner],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {
  constructor(public userService: UserService) {}
}
