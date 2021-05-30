import {AuthenticationStrategy} from '@loopback/authentication';
import {inject, service} from '@loopback/core';
import {HttpErrors} from '@loopback/rest';
import {Request} from 'express';
import * as admin from 'firebase-admin';
import {FireabaseAdminInjector} from '../injectors';
import {User} from '../models';
import {MyUserProfile} from '../models/my-user-profile';
import {UserService} from '../services';

export interface FirebaseCredentials {
  uid: string;
}

export class FirebaseStrategy implements AuthenticationStrategy {
  name = 'firebase';

  constructor(
    @service(UserService) private userService: UserService,
    @inject(FireabaseAdminInjector) private firebaseAdminApp: admin.app.App,
  ) {}

  async authenticate(request: Request): Promise<MyUserProfile | undefined> {
    try {
      const credentials = await this.extractCredentials(request);
      const user = await this.userService.getOrCreateUser(credentials);
      const userProfile = await this.convertToUserProfile(user);

      return userProfile;
    } catch (err) {
      console.error(err);
      throw new HttpErrors.Unauthorized('Invalid token.');
    }
  }

  async extractCredentials(request: Request): Promise<FirebaseCredentials> {
    const tokenId = request.headers.authorization as string;

    const uid = (await this.firebaseAdminApp.auth().verifyIdToken(tokenId, true)).uid;

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
      userRecord.photoURL,
    );
  }
}
