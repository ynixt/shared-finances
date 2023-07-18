import { Field, ObjectType } from '@nestjs/graphql';
import { ChartSerie } from './chartSerie';

@ObjectType()
export class Chart {
  constructor(obj?: { name: string; series: ChartSerie[] }) {
    this.name = obj?.name;
    this.series = obj?.series;
  }

  @Field()
  name: string;

  @Field(() => [ChartSerie])
  series: ChartSerie[];
}
