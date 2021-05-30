import { arg, query, resolver } from '@loopback/graphql';
import { repository } from '@loopback/repository';
import { User } from '../models';
import { UserRepository } from '../repositories';

@resolver(of => User)
export class UserResolver {
  constructor(
    @repository(UserRepository)
    private readonly userRepository: UserRepository,
  ) {}

  @query(returns => User, { nullable: true })
  async user(@arg('userId') userId: string) {
    return this.userRepository.findById(userId);
  }

  @query(returns => [User])
  async users(): Promise<User[]> {
    return this.userRepository.find();
  }
}
