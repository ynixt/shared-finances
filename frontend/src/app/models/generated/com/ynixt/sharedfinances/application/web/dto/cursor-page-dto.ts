/* eslint-disable */
/* tslint-disable */

export interface CursorPageDto<T extends any> {
  hasNext: boolean;
  items: Array<T>;
  nextCursor?: { [key: string]: any } | null;
}
