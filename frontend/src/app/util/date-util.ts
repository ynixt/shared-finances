import dayjs, { Dayjs } from 'dayjs';

export const ONLY_DATE_FORMAT = 'YYYY-MM-DD';

export const skipWeekend = (date: Dayjs): Dayjs => {
  const day = date.day();

  if (day === 6) return date.add(2, 'day');
  if (day === 0) return date.add(1, 'day');

  return date;
};

export const parseYearMonth = (value: string): Dayjs | undefined => {
  if (/^\d{4}-\d{2}$/.test(value)) {
    const parsed = dayjs(`${value}-01`);
    return parsed.isValid() ? parsed : undefined;
  }

  if (/^\d{2}-\d{4}$/.test(value)) {
    const [month, year] = value.split('-');
    const parsed = dayjs(`${year}-${month}-01`);
    return parsed.isValid() ? parsed : undefined;
  }

  return undefined;
};
