import { ArgsType, Field } from '@nestjs/graphql';

@ArgsType()
export class EditCategoryArgs {
  @Field()
  id: string;

  @Field()
  name: string;

  @Field()
  color: string;
}
