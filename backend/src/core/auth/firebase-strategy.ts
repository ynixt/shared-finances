import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt } from 'passport-jwt';
import { FirebaseAuthStrategy, FirebaseUser } from '@tfarras/nestjs-firebase-auth';
import { UserService } from '../user/user.service';

export interface FBUser extends FirebaseUser {
  id: string;
  groupsId: string[];
}

@Injectable()
export class FirebaseStrategy extends PassportStrategy(FirebaseAuthStrategy, 'firebase') {
  public constructor(private userService: UserService) {
    super({
      extractor: ExtractJwt.fromAuthHeaderAsBearerToken(),
      checkRevoked: true,
    });
  }

  async validate(payload: FirebaseUser): Promise<FBUser> {
    const user = await this.userService.getOrCreateUser(payload.uid);

    return {
      ...payload,
      id: user.id,
      groupsId: [...user.groupsId].map(objectId => (objectId as any).toHexString()),
    };
  }
}
