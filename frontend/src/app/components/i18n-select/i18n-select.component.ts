import { Component, forwardRef, input } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { Select } from 'primeng/select';

import { SimpleMenuItem } from '../../models/simple-menu-item';
import { SimpleControlValueAccessor } from '../simple-control-value-accessor';

@Component({
  selector: 'app-i18n-select',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, Select, TranslatePipe],
  templateUrl: './i18n-select.component.html',
  styleUrl: './i18n-select.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => I18nSelectComponent),
      multi: true,
    },
  ],
})
export class I18nSelectComponent extends SimpleControlValueAccessor<any> {
  options = input<SimpleMenuItem<any>[]>([]);

  constructor() {
    super();
  }
}
