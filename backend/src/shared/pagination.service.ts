import { Injectable } from '@nestjs/common';
import { Query, Document } from 'mongoose';

export interface IPagination {
  page: number;
  pageSize: number;
}

export const MAX_PAGE_SIZE = 20;
export const INITIAL_PAGE = 1;

export class Pagination implements IPagination {
  page = INITIAL_PAGE;
  pageSize: number = MAX_PAGE_SIZE;

  get skip() {
    return (this.page - 1) * this.pageSize;
  }

  constructor(opts?: Partial<IPagination>) {
    if (opts) {
      if (opts.page) {
        this.page = opts.page < 1 ? 1 : opts.page;
      }

      if (opts.pageSize) {
        if (opts.pageSize > MAX_PAGE_SIZE) {
          this.pageSize = MAX_PAGE_SIZE;
        } else if (opts.pageSize < 0) {
          this.pageSize = 0;
        } else {
          this.pageSize = opts.pageSize;
        }
      }
    }
  }
}

export interface IPage<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

@Injectable()
export class PaginationService {
  async convertQueryToPage<TDocument extends Document>(
    query: Query<TDocument[], TDocument>,
    pagination: Pagination,
  ): Promise<IPage<TDocument>> {
    const total = await query.count().exec();

    query.limit(pagination.pageSize);
    query.skip(pagination.skip);

    const items = await query.find().exec();

    return {
      items,
      total,
      page: pagination.page,
      pageSize: pagination.pageSize,
    };
  }
}
