import { Component, forwardRef, input, signal } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { Textarea } from 'primeng/textarea';

import { SimpleControlValueAccessor } from '../simple-control-value-accessor';

@Component({
  selector: 'app-textarea',
  imports: [FormsModule, ReactiveFormsModule, Textarea],
  templateUrl: './textarea.component.html',
  styleUrl: './textarea.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TextareaComponent),
      multi: true,
    },
  ],
})
export class TextareaComponent extends SimpleControlValueAccessor<string> {
  rows = input<number>(2);
  maxLength = input<number | undefined>(undefined);
  showLength = input<boolean>(true);
  autoResize = input<boolean>(true);
  currentLength = signal<number>(this.value?.length ?? 0);

  override onValueChange(value: string | undefined) {
    const maxLength = this.maxLength();

    if (maxLength != null && value != null && value?.length > maxLength) {
      value = value.substring(0, maxLength);
      this.writeValue(value);
    }

    super.onValueChange(value);

    this.currentLength.set(value?.length ?? 0);
  }
}
