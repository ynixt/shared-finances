import { forwardRef, Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { GroupInvite, GroupInviteSchema, Group, GroupSchema } from '../models';
import { TransactionModule } from '../transaction';
import { UserModule } from '../user/user.module';
import { GroupInviteRepository } from './group-invite.repository';
import { GroupSummaryExpenseResolver } from './group-summary-expense.resolver';
import { GroupRepository } from './group.repository';
import { GroupResolver } from './group.resolver';
import { GroupService } from './group.service';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Group.name, schema: GroupSchema },
      { name: GroupInvite.name, schema: GroupInviteSchema },
    ]),
    forwardRef(() => UserModule),
    forwardRef(() => TransactionModule),
  ],
  providers: [GroupService, GroupResolver, GroupRepository, GroupInviteRepository, GroupSummaryExpenseResolver],
  exports: [GroupService],
})
export class GroupModule {}
