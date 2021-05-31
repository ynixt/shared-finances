import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt } from 'passport-jwt';
import { FirebaseAuthStrategy, FirebaseUser } from '@tfarras/nestjs-firebase-auth';
import { UserService } from '../user/user.service';

export interface FirebaseUserWithId extends FirebaseUser {
  id: string;
}

@Injectable()
export class FirebaseStrategy extends PassportStrategy(FirebaseAuthStrategy, 'firebase') {
  public constructor(private userService: UserService) {
    super({
      extractor: ExtractJwt.fromAuthHeaderAsBearerToken(),
      checkRevoked: true,
    });
  }

  async validate(payload: FirebaseUser): Promise<FirebaseUserWithId> {
    const user = await this.userService.getOrCreateUser(payload.uid);

    return {
      ...payload,
      id: user.id,
    };
  }
}
