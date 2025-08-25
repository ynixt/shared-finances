import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';

import { NavbarComponent } from '../../components/navbar/navbar.component';

@Component({
  selector: 'app-not-found-page',
  imports: [NavbarComponent, TranslatePipe, RouterLink, ButtonDirective],
  templateUrl: './not-found-page.component.html',
  styleUrl: './not-found-page.component.scss',
})
export class NotFoundPageComponent {}
