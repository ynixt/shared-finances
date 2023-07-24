import { ControlContainer, ControlValueAccessor, UntypedFormControl } from '@angular/forms';
import { Component, Input } from '@angular/core';

@Component({ template: `` })
export abstract class ControlValueAccessorConnector<T> implements ControlValueAccessor {
  value: T;
  onChange = (_: T) => {};
  onTouched = () => {};

  @Input() formControl: UntypedFormControl;
  @Input() formControlName: string;

  get control() {
    return this.formControl || (this.controlContainer.control.get(this.formControlName) as UntypedFormControl);
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
