import { Component, forwardRef, input } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCalendar } from '@fortawesome/free-solid-svg-icons';

import dayjs from 'dayjs';
import { DatePicker } from 'primeng/datepicker';
import { DatePickerTypeView } from 'primeng/types/datepicker';

import { SimpleControlValueAccessor } from '../simple-control-value-accessor';

@Component({
  selector: 'app-date-picker',
  imports: [DatePicker, FormsModule, FaIconComponent],
  templateUrl: './date-picker.component.html',
  styleUrl: './date-picker.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatePickerComponent),
      multi: true,
    },
  ],
})
export class DatePickerComponent extends SimpleControlValueAccessor<Date> {
  readonly clockIcon = faCalendar;

  showButtonBar = input<boolean>(true);
  readonlyInput = input<boolean>(false);
  view = input<DatePickerTypeView>('date');
  dateFormat = input<string | undefined>(undefined);

  /** PrimeNG expects `Date`; callers may still pass dayjs from domain defaults. */
  override writeValue(obj: Date | dayjs.Dayjs | undefined): void {
    if (obj == null) {
      super.writeValue(undefined);
      return;
    }
    const asDate = dayjs.isDayjs(obj) ? obj.toDate() : obj instanceof Date ? obj : new Date(obj as unknown as string | number);
    super.writeValue(asDate);
  }
}
