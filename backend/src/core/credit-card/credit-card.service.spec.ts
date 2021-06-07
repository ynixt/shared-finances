import { Test, TestingModule } from '@nestjs/testing';
import { CreditCardService } from './credit-card.service';

describe('CreditCardService', () => {
  let service: CreditCardService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [CreditCardService],
    }).compile();

    service = module.get<CreditCardService>(CreditCardService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
