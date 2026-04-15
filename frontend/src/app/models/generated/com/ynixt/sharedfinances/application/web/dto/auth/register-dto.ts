/* eslint-disable */
/* tslint-disable */

export interface RegisterDto {
  acceptPrivacy?: boolean | null;
  acceptTerms?: boolean | null;
  defaultCurrency: string;
  email: string;
  firstName: string;
  gravatarOptIn: boolean;
  lang: string;
  lastName: string;
  password: string;
  tmz: string;
  turnstileToken?: string | null;
}
