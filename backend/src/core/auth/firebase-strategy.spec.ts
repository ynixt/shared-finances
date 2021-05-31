import { Test, TestingModule } from '@nestjs/testing';
import { FirebaseStrategy } from './firebase-strategy';

describe('FirebaseStrategy', () => {
  let provider: FirebaseStrategy;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [FirebaseStrategy],
    }).compile();

    provider = module.get<FirebaseStrategy>(FirebaseStrategy);
  });

  it('should be defined', () => {
    expect(provider).toBeDefined();
  });
});
