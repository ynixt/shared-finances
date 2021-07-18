import { forwardRef, Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { BankAccountModule } from '../bank-account';
import { CategoryModule } from '../category';
import { CreditCardModule } from '../credit-card';
import { GroupModule } from '../group';
import { TransacationSchema, Transaction } from '../models';
import { TransactionChartService } from './transaction-chart.service';
import { TransactionRepository } from './transaction.repository';
import { TransactionResolver } from './transaction.resolver';
import { TransactionService } from './transaction.service';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Transaction.name, schema: TransacationSchema }]),
    forwardRef(() => BankAccountModule),
    forwardRef(() => CategoryModule),
    forwardRef(() => CreditCardModule),
    GroupModule,
  ],
  providers: [TransactionRepository, TransactionResolver, TransactionService, TransactionResolver, TransactionChartService],
  exports: [TransactionService, TransactionChartService],
})
export class TransactionModule {}
