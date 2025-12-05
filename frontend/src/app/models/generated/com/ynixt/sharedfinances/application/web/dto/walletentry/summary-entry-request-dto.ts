/* eslint-disable */
/* tslint-disable */
import { EntrySummaryType } from '../../../../domain/enums/entry-summary-type';

export interface SummaryEntryRequestDto {
  groupId?: string | null;
  maximumDate?: string | null;
  minimumDate?: string | null;
  summaryType: EntrySummaryType;
  walletItemId?: string | null;
}
