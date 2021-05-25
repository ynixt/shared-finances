import {AuthenticationStrategy} from '@loopback/authentication';
import {inject, service} from '@loopback/core';
import {UserProfile} from '@loopback/security';
import * as admin from 'firebase-admin';
import {Request} from 'express';

import {FireabaseAdminInjector} from '..';
import {UserService} from '../services';
import {HttpErrors} from '@loopback/rest';
import {User} from '../models';
import {MyUserProfile} from '../models/my-user-profile';

export interface FirebaseCredentials {
  uid: string;
}

export class FirebaseStrategy implements AuthenticationStrategy {
  name: string = 'firebase';

  constructor(
    @service(UserService) private userService: UserService,
    @inject(FireabaseAdminInjector) private firebaseAdminApp: admin.app.App,
  ) {}

  async authenticate(request: Request): Promise<MyUserProfile | undefined> {
    try {
      const credentials = await this.extractCredentials(request);
      const user = await this.userService.getOrCreateUser(credentials);
      const userProfile = this.convertToUserProfile(user);

      return userProfile;
    } catch (err) {
      console.error(err);
      throw new HttpErrors.Unauthorized('Invalid email or password.');
    }
  }

  async extractCredentials(request: Request): Promise<FirebaseCredentials> {
    const tokenId = request.headers['token-id'] as string;

    const uid = (await this.firebaseAdminApp.auth().verifyIdToken(tokenId)).uid;

    return {
      uid,
    };
  }

  public async convertToUserProfile(user: User): Promise<MyUserProfile> {
    const userRecord = await this.firebaseAdminApp.auth().getUser(user.uid);

    return new MyUserProfile(
      user.id,
      userRecord.uid,
      userRecord.email,
      userRecord.displayName,
    );
  }
}
