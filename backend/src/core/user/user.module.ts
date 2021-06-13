import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { BankAccountModule } from '../bank-account';
import { User, UserSchema } from '../models';
import { UserRepository } from './user.repository';
import { UserResolver } from './user.resolver';
import { UserService } from './user.service';

@Module({
  imports: [MongooseModule.forFeature([{ name: User.name, schema: UserSchema }]), BankAccountModule],
  providers: [UserService, UserResolver, UserRepository],
  exports: [UserService],
})
export class UserModule {}
