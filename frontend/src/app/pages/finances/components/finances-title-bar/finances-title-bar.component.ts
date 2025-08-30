import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faChevronLeft, faPlus } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';

@Component({
  selector: 'app-finances-title-bar',
  imports: [FaIconComponent, RouterLink, ButtonDirective, TranslatePipe, Tooltip],
  templateUrl: './finances-title-bar.component.html',
  styleUrl: './finances-title-bar.component.scss',
})
export class FinancesTitleBarComponent {
  protected readonly newButtonIcon = faPlus;
  protected readonly closeButtonIcon = faChevronLeft;

  @Input() title!: string;
  @Input() newRouterLink: string | undefined = undefined;
  @Input() closeRouterLink: string | undefined | string[] = undefined;
}
