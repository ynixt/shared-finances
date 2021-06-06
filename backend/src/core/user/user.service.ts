import { Inject, Injectable } from '@nestjs/common';
import { FirebaseAdminSDK, FIREBASE_ADMIN_INJECT } from '@tfarras/nestjs-firebase-admin';
import { User } from '../models';
import { UserRepository } from './user.repository';

@Injectable()
export class UserService {
  public static instance: UserService;

  constructor(private userRepository: UserRepository, @Inject(FIREBASE_ADMIN_INJECT) private firebaseAdmin: FirebaseAdminSDK) {
    UserService.instance = this;
  }

  public async getOrCreateUser(uid: string): Promise<User> {
    let user = await this.getUserByUid(uid);

    if (user == null) {
      user = await this.userRepository.create(uid);
    }

    return user;
  }

  public async getUserById(id: string, loadProfile = false): Promise<User> {
    const user = await this.userRepository.geById(id);

    if (user != null && loadProfile) {
      await this.loadProfile(user);
    }

    return user;
  }

  public async getUserByUid(uid: string, loadProfile = false): Promise<User> {
    const user = await this.userRepository.getByUid(uid);

    if (user != null && loadProfile) {
      await this.loadProfile(user);
    }

    return user;
  }

  public async addGroupToUser(userId: string, groupId: string): Promise<void> {
    return this.userRepository.addGroupToUser(userId, groupId);
  }

  private async loadProfile(user: User): Promise<User> {
    const firebaseUserRecord = await this.firebaseAdmin.auth().getUser(user.uid);

    user.email = firebaseUserRecord.email;
    user.name = firebaseUserRecord.displayName;
    user.photoURL = firebaseUserRecord.photoURL;

    return user;
  }
}
