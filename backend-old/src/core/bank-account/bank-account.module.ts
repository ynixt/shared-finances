import { forwardRef, Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { BankAccount, BankAccountSchema } from '../models';
import { TransactionModule } from '../transaction';
import { BankAccountRepository } from './bank-account.repository';
import { BankAccountResolver } from './bank-account.resolver';
import { BankAccountService } from './bank-account.service';

@Module({
  imports: [MongooseModule.forFeature([{ name: BankAccount.name, schema: BankAccountSchema }]), forwardRef(() => TransactionModule)],
  providers: [BankAccountRepository, BankAccountService, BankAccountResolver],
  exports: [BankAccountService],
})
export class BankAccountModule {}
