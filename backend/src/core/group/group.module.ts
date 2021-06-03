import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { GroupShareUrl, GroupShareUrlSchema, Group, GroupSchema } from '../models';
import { UserModule } from '../user/user.module';
import { GroupResolver } from './group.resolver';
import { GroupService } from './group.service';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Group.name, schema: GroupSchema },
      { name: GroupShareUrl.name, schema: GroupShareUrlSchema },
    ]),
    UserModule,
  ],
  providers: [GroupService, GroupResolver],
})
export class GroupModule {}
