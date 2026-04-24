import { AsyncPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, Injectable, inject } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faGithub } from '@fortawesome/free-brands-svg-icons/faGithub';

import { Observable, catchError, map, of, throwError } from 'rxjs';

import { ButtonDirective } from 'primeng/button';
import { Skeleton } from 'primeng/skeleton';

const repo = 'ynixt/shared-finances';

@Component({
  selector: 'app-github-stars',
  imports: [AsyncPipe, FaIconComponent, ButtonDirective, Skeleton],
  templateUrl: './github-stars.component.html',
  styleUrl: './github-stars.component.scss',
})
export class GithubStarsComponent {
  readonly faGithub = faGithub;
  readonly repoHref = `https://github.com/${repo}`;

  private githubStarsService = inject(GithubStarsService);

  stars$: Observable<number | undefined> = this.githubStarsService.getStars().pipe(catchError(() => of(undefined)));

  format(n: number) {
    return n >= 1000 ? (n / 1000).toFixed(1) + 'k' : n;
  }
}

@Injectable({ providedIn: 'root' })
export class GithubStarsService {
  private CACHE_KEY = 'shared_finances_stars';
  private CACHE_TTL = 24 * 60 * 60 * 1000; // 24h

  constructor(private http: HttpClient) {}

  getStars(): Observable<number> {
    try {
      const cached = localStorage.getItem(this.CACHE_KEY);

      if (cached) {
        const { value, timestamp } = JSON.parse(cached);

        const isValid = Date.now() - timestamp < this.CACHE_TTL;

        if (isValid) {
          return of(value);
        }
      }
    } catch (err) {
      return throwError(() => err);
    }

    return this.http.get<any>(`https://api.github.com/repos/${repo}`).pipe(
      map(r => {
        const stars = r.stargazers_count;

        localStorage.setItem(
          this.CACHE_KEY,
          JSON.stringify({
            value: stars,
            timestamp: Date.now(),
          }),
        );

        return stars;
      }),
    );
  }
}
