import { Component } from '@angular/core';

import { NavbarComponent } from '../../../components/navbar/navbar.component';
import { LangService } from '../../../services/lang.service';

@Component({
  selector: 'app-finances-home-page',
  imports: [NavbarComponent],
  templateUrl: './finances-home-page.component.html',
  styleUrl: './finances-home-page.component.scss',
})
export class FinancesHomePageComponent {
  constructor(private langService: LangService) {}
}
