import moment, { Moment } from 'moment';

export class DateUtil {
  public static dateIsBiggerThanToday(date: Moment | string) {
    return moment(date).isAfter(moment());
  }
}
