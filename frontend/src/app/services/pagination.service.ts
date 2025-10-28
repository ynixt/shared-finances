import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Observable, map } from 'rxjs';

import { Page, PageRequest, Pageable, Sort } from '../models/pagination';

export const DEFAULT_PAGE_SIZE = 20;

@Injectable({ providedIn: 'root' })
export class PaginationService {
  constructor(private httpClient: HttpClient) {}

  get<T>(url: string, pageRequest?: PageRequest, extraParams?: HttpParams | { [key: string]: any }): Observable<Page<T>> {
    let extraHttpParams = extraParams instanceof HttpParams ? extraParams : new HttpParams();

    if (!(extraParams instanceof HttpParams) && extraParams) {
      for (const key of Object.keys(extraParams)) {
        const value = extraParams[key];
        if (value !== undefined) {
          extraHttpParams = extraHttpParams.set(key, String(value!!));
        }
      }
    }

    const params = this.joinHttpParams(this.convertPageRequestIntoHttpParams(pageRequest), extraHttpParams);

    return this.httpClient
      .get(url, {
        params,
      })
      .pipe(map(raw => this.parseSpringPage<T>(raw)));
  }

  convertPageRequestIntoHttpParams(request?: PageRequest): HttpParams {
    let httpParams = new HttpParams();

    if (request == null) {
      request = {
        page: 0,
        size: DEFAULT_PAGE_SIZE,
      };
    }

    if (typeof request.page !== 'undefined') {
      httpParams = httpParams.set('page', String(request.page));
    }
    if (typeof request.size !== 'undefined') {
      httpParams = httpParams.set('size', String(request.size));
    }

    if (request.sort) {
      if (typeof request.sort === 'string') {
        httpParams = httpParams.append('sort', request.sort);
      } else {
        request.sort.forEach(s => {
          const dir = (s.direction ?? 'ASC').toLowerCase();
          httpParams = httpParams.append('sort', `${s.property},${dir}`);
        });
      }
    }

    return httpParams;
  }

  private joinHttpParams(params: HttpParams, extraParams?: HttpParams): HttpParams {
    if (extraParams != null) {
      extraParams.keys().forEach(key => {
        const values = extraParams.getAll(key);
        if (values) {
          values.forEach(value => {
            params = params!!.set(key, value);
          });
        }
      });
    }

    return params;
  }

  private parseSpringPage<T = any>(raw: any): Page<T> {
    return {
      content: Array.isArray(raw?.content) ? raw.content : [],
      pageable: raw?.pageable ?? {
        sort: raw?.sort ?? { sorted: false, unsorted: true, empty: true },
        offset: (raw?.number ?? 0) * (raw?.size ?? 0),
        pageNumber: raw?.number ?? 0,
        pageSize: raw?.size ?? 0,
        paged: true,
        unpaged: false,
      },
      totalPages: raw?.totalPages ?? 0,
      totalElements: raw?.totalElements ?? 0,
      last: raw?.last ?? false,
      size: raw?.size ?? 0,
      number: raw?.number ?? 0,
      sort: raw?.sort ?? { sorted: false, unsorted: true, empty: true },
      first: raw?.first ?? false,
      numberOfElements: raw?.numberOfElements ?? (Array.isArray(raw?.content) ? raw.content.length : 0),
      empty: raw?.empty ?? !(Array.isArray(raw?.content) && raw.content.length > 0),
    };
  }
}

export function createEmptyPage<T = any>(): Page<T> {
  const pageNumber = 0;
  const pageSize = 0;

  const emptySort: Sort = { sorted: false, unsorted: true, empty: true };

  const pageable: Pageable = {
    sort: emptySort,
    offset: pageNumber * pageSize,
    pageNumber,
    pageSize,
    paged: pageSize > 0,
    unpaged: pageSize === 0,
  };

  return {
    content: [] as T[],
    pageable,
    totalPages: 0,
    totalElements: 0,
    last: true,
    size: pageSize,
    number: pageNumber,
    sort: emptySort,
    first: true,
    numberOfElements: 0,
    empty: true,
  };
}
