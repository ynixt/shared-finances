import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { GroupShareUrl, GroupShareUrlSchema, Group, GroupSchema } from '../models';
import { GroupResolver } from './group.resolver';
import { GroupService } from './group.service';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Group.name, schema: GroupSchema },
      { name: GroupShareUrl.name, schema: GroupShareUrlSchema },
    ]),
  ],
  providers: [GroupService, GroupResolver],
})
export class GroupModule {}
