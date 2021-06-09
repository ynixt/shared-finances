import { Test, TestingModule } from '@nestjs/testing';
import { BankAccountRepository } from './bank-account.repository';

describe('BankAccountRepository', () => {
  let provider: BankAccountRepository;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [BankAccountRepository],
    }).compile();

    provider = module.get<BankAccountRepository>(BankAccountRepository);
  });

  it('should be defined', () => {
    expect(provider).toBeDefined();
  });
});
