import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CreditCard, CreditCardSchema } from '../models';
import { CreditCardRepository } from './credit-card.repository';
import { CreditCardResolver } from './credit-card.resolver';
import { CreditCardService } from './credit-card.service';

@Module({
  providers: [CreditCardRepository, CreditCardResolver, CreditCardService],
  imports: [MongooseModule.forFeature([{ name: CreditCard.name, schema: CreditCardSchema }])],
  exports: [CreditCardService],
})
export class CreditCardModule {}
