/* eslint-disable */
/* tslint-disable */
import { EntrySumDto } from './entry-sum-dto';
import { EntrySummaryGroupedDto } from './entry-summary-grouped-dto';

export interface EntrySummaryDto {
  grouped: Array<EntrySummaryGroupedDto>;
  total: EntrySumDto;
  totalPeriod: EntrySumDto;
  totalProjected: EntrySumDto;
}
