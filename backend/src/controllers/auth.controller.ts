import { authenticate } from '@loopback/authentication';
import { inject } from '@loopback/core';
import { get } from '@loopback/rest';
import { SecurityBindings, UserProfile } from '@loopback/security';

export class AuthController {
  constructor() {}

  @get('/auth/currentUser')
  @authenticate('firebase')
  async currentUser(@inject(SecurityBindings.USER, { optional: true }) user: UserProfile): Promise<UserProfile> {
    return user;
  }
}
