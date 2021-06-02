import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { Group, GroupSchema } from '../models/group';
import { GroupResolver } from './group.resolver';
import { GroupService } from './group.service';

@Module({
  imports: [MongooseModule.forFeature([{ name: Group.name, schema: GroupSchema }])],
  providers: [GroupService, GroupResolver],
})
export class GroupModule {}
