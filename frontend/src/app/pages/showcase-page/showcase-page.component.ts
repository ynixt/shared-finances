import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective, ButtonLabel } from 'primeng/button';

import { LangButtonComponent } from '../../components/lang-button/lang-button.component';
import { NavbarComponent } from '../../components/navbar/navbar.component';

@Component({
  selector: 'app-showcase-page',
  imports: [RouterLink, ButtonDirective, ButtonLabel, TranslatePipe, LangButtonComponent, NavbarComponent],
  templateUrl: './showcase-page.component.html',
  styleUrl: './showcase-page.component.scss',
})
export class ShowcasePageComponent {}
