import {injectable, BindingScope, inject} from '@loopback/core';
import {repository} from '@loopback/repository';
import {FirebaseCredentials} from '../authentication-strategies/FirebaseStrategy';
import {User} from '../models';
import {UserRepository} from '../repositories';

@injectable({scope: BindingScope.TRANSIENT})
export class UserService {
  constructor(
    @repository(UserRepository)
    private readonly userRepository: UserRepository,
  ) {}

  public async getOrCreateUser(
    credentials: FirebaseCredentials,
  ): Promise<User> {
    let user = await this.userRepository.getByUid(credentials.uid);

    if (user == null) {
      user = await this.userRepository.create({
        uid: credentials.uid,
      });
    }

    return user;
  }
}
