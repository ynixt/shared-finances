import { ArgsType, Field } from '@nestjs/graphql';

@ArgsType()
export class NewCategoryArgs {
  @Field()
  name: string;

  @Field()
  color: string;
}
