import { Inject, Injectable } from '@nestjs/common';
import { FirebaseAdminSDK, FIREBASE_ADMIN_INJECT } from '@tfarras/nestjs-firebase-admin';
import { MongoRepositoryOptions } from '../data/mongo-repository';
import { User } from '../models';
import { UserRepository } from './user.repository';

@Injectable()
export class UserService {
  constructor(private userRepository: UserRepository, @Inject(FIREBASE_ADMIN_INJECT) private firebaseAdmin: FirebaseAdminSDK) {}

  public async getOrCreateUser(uid): Promise<User> {
    let user = await this.getUserByUid(uid);

    if (user == null) {
      user = await this.createUser(uid);
    }

    return user;
  }

  public async getUserById(id: string): Promise<User> {
    return this.userRepository.getById(id);
  }

  public async getUserByUid(uid: string): Promise<User> {
    const user = await this.userRepository.getByUid(uid);

    return user;
  }

  public async addGroupToUser(userId: string, groupId: string, opts?: MongoRepositoryOptions): Promise<void> {
    return this.userRepository.addGroupToUser(userId, groupId, opts);
  }

  public getByGroupWithoutCheckPermission(groupId: string): Promise<User[]> {
    return this.userRepository.getByGroup(groupId);
  }

  private async createUser(uid: string): Promise<User> {
    const firebaseUserRecord = await this.firebaseAdmin.auth().getUser(uid);

    return this.userRepository.create({
      uid,
      name: firebaseUserRecord.displayName,
      email: firebaseUserRecord.email,
      photoURL: firebaseUserRecord.photoURL,
    });
  }
}
