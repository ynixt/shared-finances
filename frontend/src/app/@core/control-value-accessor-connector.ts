import { ControlContainer, ControlValueAccessor, FormControl } from '@angular/forms';
import { Injectable, Input } from '@angular/core';

@Injectable()
export abstract class ControlValueAccessorConnector<T> implements ControlValueAccessor {
  value: T;
  onChange = (_: T) => {};
  onTouched = () => {};

  @Input() formControl: FormControl;
  @Input() formControlName: string;

  get control() {
    return this.formControl || (this.controlContainer.control.get(this.formControlName) as FormControl);
  }

  constructor(protected controlContainer: ControlContainer) {}

  writeValue(value: T): void {
    this.value = value;
    this.onChange(value);
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
}
