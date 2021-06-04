import { Module } from '@nestjs/common';
import { DatabaseModule } from './database/database.module';
import { AuthModule } from './auth/auth.module';
import { UserModule } from './user/user.module';
import { GraphQLModule } from '@nestjs/graphql';
import { FirebaseModule } from './firebase/firebase.module';
import { GroupModule } from './group/group.module';

@Module({
  imports: [
    DatabaseModule,
    AuthModule,
    UserModule,
    GraphQLModule.forRoot({
      installSubscriptionHandlers: true,
      autoSchemaFile: 'schema.gql',
      path: '/api/graphql',
      context: ({ req, connection }) => (connection ? { req: connection.context } : { req }),
    }),
    FirebaseModule,
    GroupModule,
  ],
})
export class CoreModule {}
