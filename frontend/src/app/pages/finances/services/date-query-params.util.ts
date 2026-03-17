import { ActivatedRoute, ParamMap, Router } from '@angular/router';

import dayjs from 'dayjs';

import { ONLY_DATE_FORMAT } from '../../../util/date-util';
import { DateRange } from '../components/wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';

export type DateQueryParamMode = 'range' | 'single';

export const MONTH_QUERY_PARAM_FORMAT = 'MM-YYYY';

export const readDateRangeFromQueryParams = (queryParamMap: Pick<ParamMap, 'get'>, mode: DateQueryParamMode): DateRange | undefined => {
  const monthDate = parseMonthQueryParam(queryParamMap.get('date'));

  if (monthDate != null) {
    return createMonthDateRange(monthDate, mode);
  }

  if (mode === 'single') {
    return undefined;
  }

  const startDate = parseOnlyDate(queryParamMap.get('startDate'));
  const endDate = parseOnlyDate(queryParamMap.get('endDate'));

  if (startDate == null || endDate == null) {
    return undefined;
  }

  return {
    startDate,
    endDate,
    sameMonth: startDate.startOf('month').isSame(startDate, 'day') && startDate.endOf('month').isSame(endDate, 'day'),
  };
};

export const createMonthDateRange = (referenceDate: dayjs.Dayjs, mode: DateQueryParamMode): DateRange =>
  mode === 'single'
    ? {
        startDate: referenceDate,
        endDate: referenceDate,
        sameMonth: true,
      }
    : {
        startDate: referenceDate.startOf('month'),
        endDate: referenceDate.endOf('month'),
        sameMonth: true,
      };

export const syncDateQueryParams = async (
  route: ActivatedRoute,
  router: Router,
  dateRange: DateRange | undefined,
  mode: DateQueryParamMode,
  today: dayjs.Dayjs = dayjs(),
) => {
  const queryParams = buildDateQueryParams(dateRange, mode, today);

  if (
    route.snapshot.queryParamMap.get('date') === queryParams.date &&
    route.snapshot.queryParamMap.get('startDate') === queryParams.startDate &&
    route.snapshot.queryParamMap.get('endDate') === queryParams.endDate
  ) {
    return;
  }

  await router.navigate([], {
    relativeTo: route,
    queryParams,
    queryParamsHandling: 'merge',
    replaceUrl: true,
  });
};

const buildDateQueryParams = (dateRange: DateRange | undefined, mode: DateQueryParamMode, today: dayjs.Dayjs) => {
  const isCurrentMonth = dateRange?.startDate.isSame(today, 'month') ?? false;

  if (dateRange == null || isCurrentMonth) {
    return {
      date: null,
      startDate: null,
      endDate: null,
    };
  }

  if (mode === 'single') {
    return {
      date: dateRange.startDate.format(MONTH_QUERY_PARAM_FORMAT),
      startDate: null,
      endDate: null,
    };
  }

  if (dateRange.sameMonth) {
    return {
      date: dateRange.startDate.format(MONTH_QUERY_PARAM_FORMAT),
      startDate: null,
      endDate: null,
    };
  }

  return {
    date: null,
    startDate: dateRange.startDate.format(ONLY_DATE_FORMAT),
    endDate: dateRange.endDate?.format(ONLY_DATE_FORMAT) ?? null,
  };
};

const parseMonthQueryParam = (value: string | null) => {
  if (value == null) return undefined;

  const [month, year] = value.split('-');

  if (month == null || year == null || !/^\d{2}$/.test(month) || !/^\d{4}$/.test(year)) {
    return undefined;
  }

  const parsedMonth = Number(month);

  if (parsedMonth < 1 || parsedMonth > 12) {
    return undefined;
  }

  const parsedDate = dayjs(`${year}-${month}-01`);

  return parsedDate.isValid() ? parsedDate : undefined;
};

const parseOnlyDate = (value: string | null) => {
  if (value == null) return undefined;

  const parsedDate = dayjs(value);

  return parsedDate.isValid() ? parsedDate : undefined;
};
