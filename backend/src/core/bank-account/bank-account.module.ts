import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { User, UserSchema } from '../models';
import { BankAccountRepository } from './bank-account.repository';
import { BankAccountResolver } from './bank-account.resolver';
import { BankAccountService } from './bank-account.service';

@Module({
  imports: [MongooseModule.forFeature([{ name: User.name, schema: UserSchema }])],
  providers: [BankAccountRepository, BankAccountService, BankAccountResolver],
})
export class BankAccountModule {}
