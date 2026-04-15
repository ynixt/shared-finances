/* eslint-disable */
/* tslint-disable */

export interface ChangePendingEmailRequestDto {
  currentEmail: string;
  newEmail: string;
  turnstileToken?: string | null;
}
