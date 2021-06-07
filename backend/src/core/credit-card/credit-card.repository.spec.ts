import { Test, TestingModule } from '@nestjs/testing';
import { CreditCardRepository } from './credit-card.repository';

describe('CreditCardRepository', () => {
  let provider: CreditCardRepository;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [CreditCardRepository],
    }).compile();

    provider = module.get<CreditCardRepository>(CreditCardRepository);
  });

  it('should be defined', () => {
    expect(provider).toBeDefined();
  });
});
