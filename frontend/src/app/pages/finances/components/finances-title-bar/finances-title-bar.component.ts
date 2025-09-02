import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faChevronLeft, faPlus, faTrash } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

import { PrimeTemplate } from 'primeng/api';
import { Button, ButtonDirective } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';

@Component({
  selector: 'app-finances-title-bar',
  imports: [FaIconComponent, RouterLink, ButtonDirective, TranslatePipe, Tooltip, Button, PrimeTemplate],
  templateUrl: './finances-title-bar.component.html',
  styleUrl: './finances-title-bar.component.scss',
})
export class FinancesTitleBarComponent {
  protected readonly newButtonIcon = faPlus;
  protected readonly closeButtonIcon = faChevronLeft;
  protected readonly deleteButtonIcon = faTrash;

  @Input() title!: string;
  @Input() newRouterLink: string | undefined = undefined;
  @Input() closeRouterLink: string | undefined | string[] = undefined;
  @Input() showDeleteButton: boolean = false;
  @Input() deleteButtonLoading: boolean = false;

  @Output() deleteButtonClicked = new EventEmitter<void>();
}
