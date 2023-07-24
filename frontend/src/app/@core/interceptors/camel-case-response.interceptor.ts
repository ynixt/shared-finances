import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";

@Injectable({
  providedIn: "root"
})
export class CamelCaseResponseInterceptor implements HttpInterceptor {
  public intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (request.url.indexOf("/api") != 0) return next.handle(request);

    return next
      .handle(
        request.clone({ body: this.recursiveToSnake(request.body) }),
      )
      .pipe(
        map((event) => {
          if (event instanceof HttpResponse) {
            return event.clone({
              body: this.recursiveToCamel(event.body)
            });
          }

          return event;
        })
      );
  }

  private recursiveToCamel(item: unknown): unknown {
    if (Array.isArray(item)) {
      return item.map((el: unknown) => this.recursiveToCamel(el));
    } else if (typeof item === "function" || item !== Object(item)) {
      return item;
    }
    return Object.fromEntries(
      Object.entries(item as Record<string, unknown>).map(
        ([key, value]: [string, unknown]) => [
          key.replace(/([-_][a-z])/gi, c => c.toUpperCase().replace(/[-_]/g, "")),
          this.recursiveToCamel(value)
        ]
      )
    );
  };

  private recursiveToSnake(item: unknown): unknown {
    if (Array.isArray(item)) {
      return item.map((el: unknown) => this.recursiveToCamel(el));
    } else if (typeof item === "function" || item !== Object(item)) {
      return item;
    }
    return Object.fromEntries(
      Object.entries(item as Record<string, unknown>).map(
        ([key, value]: [string, unknown]) => [
          key.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`),
          this.recursiveToSnake(value)
        ]
      )
    );
  };
}

