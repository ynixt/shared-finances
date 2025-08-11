export interface User {
  id: string;
  externalId: string;
  email: string;
  firstName: string;
  lastName: string;
  photoUrl?: string | undefined;
  lang: string;
}
