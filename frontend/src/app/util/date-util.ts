import { Dayjs } from 'dayjs';

export const skipWeekend = (date: Dayjs): Dayjs => {
  const day = date.day();

  if (day === 6) return date.add(2, 'day');
  if (day === 0) return date.add(1, 'day');

  return date;
};
