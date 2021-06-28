export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export class Pagination {
  page = 1;
  pageSize = 20;
}
