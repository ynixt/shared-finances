import { Injectable } from '@nestjs/common';
import { CreditCard } from '../models';
import { NewCreditCardArgs } from '../models/args';
import { CreditCardRepository } from './credit-card.repository';

@Injectable()
export class CreditCardService {
  constructor(private creditCardRepository: CreditCardRepository) {}

  create(userId: string, creditCard: NewCreditCardArgs): Promise<CreditCard> {
    return this.creditCardRepository.create(userId, creditCard);
  }

  getById(userId: string, creditCardId: string): Promise<CreditCard> {
    return this.creditCardRepository.getById(userId, creditCardId);
  }

  delete(userId: string, creditCardId: string): Promise<boolean> {
    return this.creditCardRepository.delete(userId, creditCardId);
  }
}
