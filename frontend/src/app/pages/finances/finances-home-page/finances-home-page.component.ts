import { Component } from '@angular/core';

import { LangService } from '../../../services/lang.service';

@Component({
  selector: 'app-finances-home-page',
  imports: [],
  templateUrl: './finances-home-page.component.html',
  styleUrl: './finances-home-page.component.scss',
})
export class FinancesHomePageComponent {
  constructor(private langService: LangService) {}
}
