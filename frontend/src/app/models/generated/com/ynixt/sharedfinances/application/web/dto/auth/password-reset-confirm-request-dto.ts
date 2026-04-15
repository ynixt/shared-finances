/* eslint-disable */
/* tslint-disable */

export interface PasswordResetConfirmRequestDto {
  newPassword: string;
  token: string;
  turnstileToken?: string | null;
}
