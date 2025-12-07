import { TitleCasePipe } from '@angular/common';
import { Component, ViewChild, ViewEncapsulation, computed, forwardRef, inject, input } from '@angular/core';
import { FormBuilder, FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule, Validators } from '@angular/forms';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import { startWith } from 'rxjs';

import dayjs from 'dayjs';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Ripple } from 'primeng/ripple';

import { SimpleControlValueAccessor } from '../../../../../../components/simple-control-value-accessor';
import { LocalDatePipe } from '../../../../../../pipes/local-date.pipe';

export type DateRange = {
  startDate: dayjs.Dayjs;
  endDate: dayjs.Dayjs;
  sameMonth: boolean;
};

@Component({
  selector: 'app-advanced-date-picker',
  imports: [DatePicker, ReactiveFormsModule, LocalDatePipe, Button, TitleCasePipe, Ripple],
  templateUrl: './advanced-date-picker.component.html',
  styleUrl: './advanced-date-picker.component.scss',
  encapsulation: ViewEncapsulation.None,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AdvancedDatePickerComponent),
      multi: true,
    },
  ],
})
@UntilDestroy()
export class AdvancedDatePickerComponent extends SimpleControlValueAccessor<DateRange> {
  readonly formBuilder: FormBuilder = inject(FormBuilder);
  readonly form = this.formBuilder.group({
    rangeDates: [this.value, []],
    date: [this.value?.startDate, []],
  });
  readonly rangeDatesFormControl = this.form.get('rangeDates') as FormControl<Date[] | null>;
  readonly dateFormControl = this.form.get('date') as FormControl<Date | null>;
  readonly selectionMode = input<'single' | 'range'>('range');
  readonly viewMode = computed(() => {
    if (this.selectionMode() == 'single') return 'month';

    return 'date';
  });

  isToShowInput = false;

  @ViewChild('datePicker') datePicker: DatePicker | undefined;

  constructor() {
    super();

    this.rangeDatesFormControl.valueChanges.pipe(untilDestroyed(this), startWith(this.rangeDatesFormControl.value)).subscribe(value => {
      if (value == null || value.length != 2 || value.findIndex(v => v == null) != -1 || this.selectionMode() == 'single') return;

      const startDate = dayjs(value[0]);
      const endDate = dayjs(value[1]);

      this.onValueChange({
        startDate,
        endDate,
        sameMonth: this.currentDateIsFullMonth({ startDate, endDate }),
      });

      let isFullMonth = this.currentDateIsFullMonth(this.value);

      this.isToShowInput = !isFullMonth;
    });

    this.dateFormControl.valueChanges.pipe(untilDestroyed(this), startWith(this.dateFormControl.value)).subscribe(value => {
      if (value == null || this.selectionMode() == 'range') return;

      const startDate = dayjs(value);

      this.onValueChange({
        startDate,
        endDate: startDate,
        sameMonth: true,
      });

      this.isToShowInput = false;
    });
  }

  override writeValue(obj: DateRange | undefined) {
    super.writeValue(obj);

    if (this.value) {
      this.rangeDatesFormControl.setValue([this.value.startDate.toDate(), this.value.endDate.toDate()]);
      this.dateFormControl.setValue(this.value.startDate.toDate());
    } else {
      this.rangeDatesFormControl.setValue(null);
      this.dateFormControl.setValue(null);
    }
  }

  showInput() {
    this.isToShowInput = true;

    setTimeout(() => {
      this.datePicker!!.toggle();
    }, 100);
  }

  datePickerClosed() {
    this.isToShowInput = !this.currentDateIsFullMonth();
  }

  previousMonth() {
    if (this.selectionMode() == 'range') {
      const rangeDates = this.rangeDatesFormControl.value;

      if (rangeDates == null || rangeDates.length != 2) return;

      this.rangeDatesFormControl.setValue([
        dayjs(rangeDates[0]).subtract(1, 'month').startOf('month').toDate(),
        dayjs(rangeDates[1]).subtract(1, 'month').endOf('month').toDate(),
      ]);
    } else if (this.dateFormControl.value != null) {
      this.dateFormControl.setValue(dayjs(this.dateFormControl.value).subtract(1, 'month').toDate());
    }
  }

  nextMonth() {
    if (this.selectionMode() == 'range') {
      const rangeDates = this.rangeDatesFormControl.value;

      if (rangeDates == null || rangeDates.length != 2) return;

      this.rangeDatesFormControl.setValue([
        dayjs(rangeDates[0]).add(1, 'month').startOf('month').toDate(),
        dayjs(rangeDates[1]).add(1, 'month').endOf('month').toDate(),
      ]);
    } else if (this.dateFormControl.value != null) {
      console.log('asd');
      this.dateFormControl.setValue(dayjs(this.dateFormControl.value).add(1, 'month').toDate());
    }
  }

  private currentDateIsFullMonth(
    value:
      | {
          startDate: dayjs.Dayjs;
          endDate: dayjs.Dayjs;
        }
      | undefined = this.value,
  ): boolean {
    if (this.selectionMode() == 'single') return true;

    if (value != null) {
      return (
        dayjs(value.startDate).startOf('month').isSame(dayjs(value.startDate)) &&
        dayjs(value.startDate).endOf('month').isSame(dayjs(value.endDate), 'day')
      );
    }

    return false;
  }
}
