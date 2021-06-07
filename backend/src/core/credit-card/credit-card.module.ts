import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { User, UserSchema } from '../models';
import { CreditCardRepository } from './credit-card.repository';
import { CreditCardResolver } from './credit-card.resolver';
import { CreditCardService } from './credit-card.service';

@Module({
  providers: [CreditCardRepository, CreditCardResolver, CreditCardService],
  imports: [MongooseModule.forFeature([{ name: User.name, schema: UserSchema }])],
})
export class CreditCardModule {}
