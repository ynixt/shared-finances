import { Component, forwardRef, input } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faPlus } from '@fortawesome/free-solid-svg-icons';

import { ButtonDirective } from 'primeng/button';
import { Chip } from 'primeng/chip';
import { InputText } from 'primeng/inputtext';

import { SimpleControlValueAccessor } from '../simple-control-value-accessor';

@Component({
  selector: 'app-chip-editor',
  imports: [FormsModule, ReactiveFormsModule, InputText, ButtonDirective, Chip, FaIconComponent],
  templateUrl: './chip-editor.component.html',
  styleUrl: './chip-editor.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ChipEditorComponent),
      multi: true,
    },
  ],
})
export class ChipEditorComponent extends SimpleControlValueAccessor<string[]> {
  readonly faPlus = faPlus;
  currentChipOnInput: string | undefined;

  maxLength = input<number | undefined>(undefined);
  forceLowerCase = input<boolean>(true);

  addCurrentChip() {
    if (this.currentChipOnInput) {
      let newChip = this.currentChipOnInput.trim().substring(0, this.maxLength());

      if (this.forceLowerCase()) {
        newChip = newChip.toLowerCase();
      }

      const tempSet = new Set(this.value ?? []);
      tempSet.add(newChip);

      this.onValueChange([...tempSet.values()]);

      this.currentChipOnInput = undefined;
    }
  }

  chipRemoved(chip: string) {
    this.onValueChange(this.value?.filter(v => v != chip));
  }
}
