import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { GroupInvite, GroupInviteSchema, Group, GroupSchema } from '../models';
import { UserModule } from '../user/user.module';
import { GroupInviteRepository } from './group-invite.repository';
import { GroupRepository } from './group.repository';
import { GroupResolver } from './group.resolver';
import { GroupService } from './group.service';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Group.name, schema: GroupSchema },
      { name: GroupInvite.name, schema: GroupInviteSchema },
    ]),
    UserModule,
  ],
  providers: [GroupService, GroupResolver, GroupRepository, GroupInviteRepository],
})
export class GroupModule {}
