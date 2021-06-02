import { Inject, Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { FirebaseAdminSDK, FIREBASE_ADMIN_INJECT } from '@tfarras/nestjs-firebase-admin';
import { Model } from 'mongoose';
import { User, UserDocument } from '../models';

@Injectable()
export class UserService {
  constructor(
    @InjectModel(User.name) private userModel: Model<UserDocument>,
    @Inject(FIREBASE_ADMIN_INJECT) private firebaseAdmin: FirebaseAdminSDK,
  ) {}

  public async getOrCreateUser(uid: string): Promise<User> {
    let user = await this.getUserByUid(uid);

    if (user == null) {
      const createdUser = await this.userModel.create({
        uid: uid,
      });

      user = await this.getUserById(createdUser.id);
    }

    return user;
  }

  public async getUserById(id: string): Promise<User> {
    const user = await this.userModel.findById(id);

    if (user != null) {
      await this.loadProfile(user);
    }

    return user;
  }

  public async getUserByUid(uid: string, loadProfile = false): Promise<User> {
    const user = await this.userModel.findOne({ uid });

    if (user != null && loadProfile) {
      await this.loadProfile(user);
    }

    return user;
  }

  private async loadProfile(user: User): Promise<User> {
    const firebaseUserRecord = await this.firebaseAdmin.auth().getUser(user.uid);

    user.email = firebaseUserRecord.email;
    user.name = firebaseUserRecord.displayName;
    user.photoURL = firebaseUserRecord.photoURL;

    return user;
  }
}
