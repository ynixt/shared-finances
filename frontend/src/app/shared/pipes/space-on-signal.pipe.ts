import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'spaceOnSignal',
})
export class SpaceOnSignalPipe implements PipeTransform {
  transform(value: string): string {
    return value.charAt(0) === '-' ? `- ${value.substring(1, value.length)}` : value;
  }
}
