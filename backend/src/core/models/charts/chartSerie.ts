import { Field, Float, ObjectType } from '@nestjs/graphql';

@ObjectType()
export class ChartSerie {
  constructor(obj?: { name: string; value: number }) {
    this.name = obj?.name;
    this.value = obj?.value;
  }

  @Field()
  name: string;

  @Field(() => Float)
  value: number;
}
