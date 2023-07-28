import { DEFAULT_PAGE_SIZE } from '../constants';

export interface Page<T> {
  content: T[];
  total: number;
  number: number;
  totalPages: number;
}

export class Pagination {
  page = 1;
  size = DEFAULT_PAGE_SIZE;
}
