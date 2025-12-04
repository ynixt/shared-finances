import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Observable, map } from 'rxjs';

import { CursorPage, CursorPageRequest } from '../models/cursor-pagination';
import { Page, PageRequest, Pageable, Sort } from '../models/pagination';

export const DEFAULT_PAGE_SIZE = 20;

@Injectable({ providedIn: 'root' })
export class CursorPaginationService {
  constructor(private httpClient: HttpClient) {}

  post<T>(
    url: string,
    pageRequest?: CursorPageRequest | undefined,
    extraParams?: HttpParams | { [key: string]: any } | undefined,
    extraBody?:
      | {
          [key: string]: any;
        }
      | undefined,
  ): Observable<CursorPage<T>> {
    const params = extraParams;

    const body = {
      ...(extraBody ?? {}),
      pageRequest: pageRequest,
    };

    if (!body.pageRequest) {
      body.pageRequest = {};
    }

    if (body.pageRequest.size == undefined) {
      body.pageRequest.size = DEFAULT_PAGE_SIZE;
    }

    return this.httpClient
      .post(url, body, {
        params,
      })
      .pipe(map(raw => raw as CursorPage<T>));
  }
}
