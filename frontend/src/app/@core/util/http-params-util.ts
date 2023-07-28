import { HttpParams } from "@angular/common/http";

export function transformObjInHttpParams(obj: { [key: string]: any; }, removeNulls = true): HttpParams {
  obj = { ...obj };

  if (removeNulls) {
    for (const key of Object.keys(obj)) {
      if (obj[key] == null) {
        delete obj[key];
      }
    }
  }

  return new HttpParams({
    fromObject: obj
  });
}

export function addHttpParamsIntoUrl(url: string, obj: { [key: string]: any; }, removeNulls = true): string {
  const httpParams = transformObjInHttpParams(obj, removeNulls);

  if (url.charAt(url.length - 1) != "?") {
    url += "?";
  }

  url += httpParams.toString();

  return url;
}
