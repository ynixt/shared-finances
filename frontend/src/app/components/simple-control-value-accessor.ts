import { ControlValueAccessor } from '@angular/forms';

export abstract class SimpleControlValueAccessor<T> implements ControlValueAccessor {
  value: T | undefined;
  disabled = false;

  protected onChange = (_: T) => {};
  protected onTouched = () => {};

  writeValue(obj: any): void {
    this.value = obj;
  }

  onValueChange(value: any) {
    this.value = value;
    this.onChange(value);
    this.onTouched();
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}
