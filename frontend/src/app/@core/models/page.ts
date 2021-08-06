import { DEFAULT_PAGE_SIZE } from '../constants';

export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export class Pagination {
  page = 1;
  pageSize = DEFAULT_PAGE_SIZE;
}
