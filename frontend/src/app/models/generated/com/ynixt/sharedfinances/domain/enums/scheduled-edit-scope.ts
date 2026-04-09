/* eslint-disable */
/* tslint-disable */

export type ScheduledEditScope = 'ONLY_THIS' | 'THIS_AND_FUTURE' | 'ALL_SERIES';

export const ScheduledEditScope__Options: ScheduledEditScope[] = ['ONLY_THIS', 'THIS_AND_FUTURE', 'ALL_SERIES'];

export const ScheduledEditScope__Obj: { [K in ScheduledEditScope]: ScheduledEditScope } = {
  'ONLY_THIS': 'ONLY_THIS',
  'THIS_AND_FUTURE': 'THIS_AND_FUTURE',
  'ALL_SERIES': 'ALL_SERIES',
};
