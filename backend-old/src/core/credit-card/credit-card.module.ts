import { forwardRef, Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CreditCard, CreditCardSchema } from '../models';
import { TransactionModule } from '../transaction';
import { CreditCardRepository } from './credit-card.repository';
import { CreditCardResolver } from './credit-card.resolver';
import { CreditCardService } from './credit-card.service';

@Module({
  providers: [CreditCardRepository, CreditCardResolver, CreditCardService],
  imports: [MongooseModule.forFeature([{ name: CreditCard.name, schema: CreditCardSchema }]), forwardRef(() => TransactionModule)],
  exports: [CreditCardService],
})
export class CreditCardModule {}
