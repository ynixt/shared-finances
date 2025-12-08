import { NgTemplateOutlet } from '@angular/common';
import { Component, ContentChild, TemplateRef, ViewEncapsulation, input } from '@angular/core';
import { FaIconComponent, IconDefinition } from '@fortawesome/angular-fontawesome';

import { Skeleton } from 'primeng/skeleton';

@Component({
  selector: 'app-dashboard-card',
  imports: [FaIconComponent, Skeleton, NgTemplateOutlet],
  templateUrl: './dashboard-card.component.html',
  styleUrl: './dashboard-card.component.scss',
  encapsulation: ViewEncapsulation.None,
})
export class DashboardCardComponent {
  titleKey = input<string | undefined>();
  value = input<string | undefined>();
  faIcon = input<IconDefinition | undefined>();
  titleCssColorClass = input<string>('text-muted-color');
  iconCssColorClass = input<string>('text-muted-color');
  valueCssColorClass = input<string>('text-surface-900 dark:text-surface-0');

  @ContentChild('headerRight', { read: TemplateRef }) headerRightTemplate?: TemplateRef<any>;
}
