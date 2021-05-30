import {securityId, UserProfile} from '@loopback/security';

export class MyUserProfile implements UserProfile {
  [securityId]: string;

  constructor(
    public id: string,
    public uid: string,
    public email?: string,
    public name?: string,
    public photoURL?: string,
  ) {
    this[securityId] = id;
  }
}
