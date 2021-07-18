import { forwardRef, Inject, Injectable } from '@nestjs/common';
import { CreditCard } from '../models';
import { EditCreditCardArgs, NewCreditCardArgs } from '../models/args';
import { TransactionService } from '../transaction';
import { CreditCardRepository } from './credit-card.repository';

@Injectable()
export class CreditCardService {
  constructor(
    private creditCardRepository: CreditCardRepository,
    @Inject(forwardRef(() => TransactionService)) private transactionService: TransactionService,
  ) {}

  create(userId: string, creditCard: NewCreditCardArgs): Promise<CreditCard> {
    return this.creditCardRepository.create({ ...creditCard, userId });
  }

  getById(userId: string, creditCardId: string): Promise<CreditCard> {
    return this.creditCardRepository.findByUserId(userId, creditCardId);
  }

  edit(userId: string, creditCard: EditCreditCardArgs): Promise<CreditCard> {
    return this.creditCardRepository.edit(userId, creditCard);
  }

  delete(userId: string, creditCardId: string): Promise<CreditCard> {
    return this.creditCardRepository.runInsideTransaction<CreditCard>(async opts => {
      const creditCard = await this.creditCardRepository.delete(userId, creditCardId, opts);

      await this.transactionService.deleteByCreditCardId(userId, creditCardId, opts);

      return creditCard;
    });
  }

  existsWithUserId(userId: string, creditCardId: string): Promise<boolean> {
    return this.creditCardRepository.existsWithUserId(userId, creditCardId);
  }

  findAllWithUserId(userId: string): Promise<CreditCard[]> {
    return this.creditCardRepository.findAllWithUserId(userId);
  }
}
