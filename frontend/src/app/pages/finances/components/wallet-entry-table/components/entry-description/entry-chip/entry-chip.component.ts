import { Component, input } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { IconDefinition } from '@fortawesome/angular-fontawesome';

import { Chip } from 'primeng/chip';

@Component({
  selector: 'app-entry-chip',
  imports: [Chip, FaIconComponent],
  templateUrl: './entry-chip.component.html',
  styleUrl: './entry-chip.component.scss',
})
export class EntryChipComponent {
  text = input<string>('');
  subtext = input<string | undefined>(undefined);
  icon = input<IconDefinition | undefined>(undefined);
}
