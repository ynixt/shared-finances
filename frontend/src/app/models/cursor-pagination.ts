export interface CursorPageRequest {
  size?: number | undefined;
  nextCursor?: { [K in string]: any } | undefined;
}

export interface CursorPage<T> {
  items: Array<T>;
  hasNext: boolean;
  nextCursor: { [K in string]: any } | undefined;
}
