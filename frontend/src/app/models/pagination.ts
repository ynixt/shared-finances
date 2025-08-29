export type SortDirection = 'ASC' | 'DESC';

export interface SortOrder {
  property: string;
  direction?: SortDirection;
  ignoreCase?: boolean;
  nullHandling?: 'NATIVE' | 'NULLS_FIRST' | 'NULLS_LAST';
  ascending?: boolean;
  descending?: boolean;
}

export interface Sort {
  sorted: boolean;
  unsorted: boolean;
  empty: boolean;
  orders?: SortOrder[];
}

export interface PageRequest {
  page?: number;
  size?: number;
  sort?: string | Array<{ property: string; direction?: SortDirection }>;
}

export interface Pageable {
  sort: Sort;
  offset: number;
  pageNumber: number;
  pageSize: number;
  paged: boolean;
  unpaged: boolean;
}

export interface Page<T> {
  content: T[];
  pageable: Pageable;
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: Sort;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}
