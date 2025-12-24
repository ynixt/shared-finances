import { Component, forwardRef } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { getTimeZones } from '@vvo/tzdb';

import { Select } from 'primeng/select';

import { SimpleControlValueAccessor } from '../simple-control-value-accessor';

export type TimeZoneItem = {
  name: string;
  all: string;
  abbreviation: string;
};

export const allTimezones = getTimeZones({ includeUtc: true })
  .map(tmz => ({
    name: tmz.name,
    abbreviation: tmz.abbreviation,
    all: tmz.name + ' ' + tmz.abbreviation,
  }))
  .sort((a, b) => a.name.localeCompare(b.name));

@Component({
  selector: 'app-time-zone-selector',
  templateUrl: './time-zone-selector.component.html',
  styleUrl: './time-zone-selector.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeZoneSelectorComponent),
      multi: true,
    },
  ],
  imports: [FormsModule, Select],
})
export class TimeZoneSelectorComponent extends SimpleControlValueAccessor<string> {
  timezones: TimeZoneItem[] = allTimezones;
}
