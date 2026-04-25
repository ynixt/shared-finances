import { Component, Input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { Button } from 'primeng/button';

import { DarkModeService } from '../../services/dark-mode.service';

@Component({
  selector: 'app-dark-mode-button',
  imports: [Button, TranslatePipe],
  templateUrl: './dark-mode-button.component.html',
})
export class DarkModeButtonComponent {
  @Input() rounded = true;
  @Input() text = true;
  @Input() buttonClass: string | undefined;

  constructor(public darkModeService: DarkModeService) {}

  toggleDarkMode() {
    this.darkModeService.toggleDarkMode();
  }
}
