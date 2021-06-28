import { AfterViewInit, Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { MatDatepicker } from '@angular/material/datepicker';
import moment, { Moment } from 'moment';

@Component({
  selector: 'app-month-bar',
  templateUrl: './month-bar.component.html',
  styleUrls: ['./month-bar.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class MonthBarComponent implements OnInit, AfterViewInit {
  private _date: Moment;
  private viewInitted = false;

  @Input() set date(date: Moment) {
    if (this.viewInitted) {
      this._date = date ?? moment();
    } else {
      this._date = date;
    }

    this.dateChange.next(this.date);
  }
  get date() {
    return this._date;
  }

  @Output() dateChange = new EventEmitter<Moment>();

  constructor() {}

  ngOnInit(): void {}

  ngAfterViewInit() {
    this.viewInitted = true;

    setTimeout(() => {
      this.date ??= moment();
    });
  }

  chosenMonthHandler(normalizedMonth: Moment, datepicker: MatDatepicker<Moment>) {
    this.date = moment(this.date).month(normalizedMonth.month());
    datepicker.close();
  }

  nextMonth(): void {
    this.date = moment(this.date).add(1, 'month');
  }

  previousMonth(): void {
    this.date = moment(this.date).add(-1, 'month');
  }
}
