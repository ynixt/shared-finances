import {UserProfile, securityId} from '@loopback/security';

export class MyUserProfile implements UserProfile {
  [securityId]: string;

  constructor(
    id: string,
    public uid: string,
    public email?: string,
    public name?: string,
  ) {
    this[securityId] = id;
  }
}
