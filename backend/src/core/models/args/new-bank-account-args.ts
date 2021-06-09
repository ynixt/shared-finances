import { ArgsType, Field } from '@nestjs/graphql';

@ArgsType()
export class NewBankAccountArgs {
  id?: string;

  @Field()
  name: string;
}
