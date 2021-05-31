import { Module } from '@nestjs/common';
import { FirebaseStrategy } from './firebase-strategy';
import { UserModule } from '../user/user.module';

@Module({
  imports: [UserModule],
  providers: [FirebaseStrategy],
})
export class AuthModule {}
