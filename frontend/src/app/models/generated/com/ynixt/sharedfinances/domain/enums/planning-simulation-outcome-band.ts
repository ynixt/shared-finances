/* eslint-disable */
/* tslint-disable */

export type PlanningSimulationOutcomeBand =
  | 'FITS'
  | 'FITS_BUT_CANNOT_SUSTAIN_SCHEDULED_GOAL_CONTRIBUTIONS'
  | 'FITS_IF_GOAL_ALLOCATIONS_ARE_REDUCED'
  | 'DOES_NOT_FIT';

export const PlanningSimulationOutcomeBand__Options: PlanningSimulationOutcomeBand[] = [
  'FITS',
  'FITS_BUT_CANNOT_SUSTAIN_SCHEDULED_GOAL_CONTRIBUTIONS',
  'FITS_IF_GOAL_ALLOCATIONS_ARE_REDUCED',
  'DOES_NOT_FIT',
];

export const PlanningSimulationOutcomeBand__Obj: { [K in PlanningSimulationOutcomeBand]: PlanningSimulationOutcomeBand } = {
  'FITS': 'FITS',
  'FITS_BUT_CANNOT_SUSTAIN_SCHEDULED_GOAL_CONTRIBUTIONS': 'FITS_BUT_CANNOT_SUSTAIN_SCHEDULED_GOAL_CONTRIBUTIONS',
  'FITS_IF_GOAL_ALLOCATIONS_ARE_REDUCED': 'FITS_IF_GOAL_ALLOCATIONS_ARE_REDUCED',
  'DOES_NOT_FIT': 'DOES_NOT_FIT',
};
