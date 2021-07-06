import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

@Component({
  selector: 'app-options-bar',
  templateUrl: './options-bar.component.html',
  styleUrls: ['./options-bar.component.scss'],
})
export class OptionsBarComponent implements OnInit {
  @Input() title = '';
  @Input() currentIndex = 0;
  @Input() formatValue: (input: any) => string = (_: any) => '';
  @Input() values: any[] = [];

  @Output() valueChange = new EventEmitter<any>();

  constructor() {}

  ngOnInit(): void {}

  previousValue(): void {
    if (this.currentIndex > 0) {
      this.changeIndex(this.currentIndex - 1);
    }
  }

  nextValue(): void {
    if (this.currentIndex < this.values.length - 1) {
      this.changeIndex(this.currentIndex + 1);
    }
  }

  changeIndex(newIndex: number): void {
    this.currentIndex = newIndex;
    this.valueChange.next(this.values[this.currentIndex]);
  }
}
