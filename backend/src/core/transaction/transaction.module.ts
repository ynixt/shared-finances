import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { BankAccountModule } from '../bank-account';
import { TransacationSchema, Transaction } from '../models';
import { TransactionRepository } from './transaction.repository';
import { TransactionResolver } from './transaction.resolver';
import { TransactionService } from './transaction.service';

@Module({
  imports: [MongooseModule.forFeature([{ name: Transaction.name, schema: TransacationSchema }]), BankAccountModule],
  providers: [TransactionRepository, TransactionResolver, TransactionService, TransactionResolver],
})
export class TransactionModule {}
