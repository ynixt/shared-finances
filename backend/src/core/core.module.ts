import { Module } from '@nestjs/common';
import { DatabaseModule } from './database/database.module';
import { AuthModule } from './auth/auth.module';
import { UserModule } from './user/user.module';
import { GraphQLModule } from '@nestjs/graphql';
import { FirebaseModule } from './firebase/firebase.module';

@Module({
  imports: [
    DatabaseModule,
    AuthModule,
    UserModule,
    GraphQLModule.forRoot({
      installSubscriptionHandlers: true,
      autoSchemaFile: 'schema.gql',
      path: '/api/graphql',
    }),
    FirebaseModule,
  ],
})
export class CoreModule {}
