import { Field, Int, ObjectType } from '@nestjs/graphql';
import { IPage } from 'src/shared';
import { Transaction } from './transaction';

@ObjectType()
export class TransactionsPage implements IPage<Transaction> {
  constructor(page: IPage<Transaction>) {
    this.items = page.items;
    this.total = page.total;
    this.page = page.page;
    this.pageSize = page.pageSize;
  }

  @Field(() => [Transaction])
  items: Transaction[];

  @Field(() => Int)
  total: number;

  @Field(() => Int)
  page: number;

  @Field(() => Int)
  pageSize: number;
}
