import { Injectable, OnDestroy, computed, signal } from '@angular/core';

export type TailwindBreakpoint = 'base' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';

interface BreakpointConfig {
  name: TailwindBreakpoint;
  query: string;
}

const TAILWIND_BREAKPOINTS: BreakpointConfig[] = [
  { name: 'sm', query: '(min-width: 640px)' },
  { name: 'md', query: '(min-width: 768px)' },
  { name: 'lg', query: '(min-width: 1024px)' },
  { name: 'xl', query: '(min-width: 1280px)' },
  { name: '2xl', query: '(min-width: 1536px)' },
];

@Injectable({ providedIn: 'root' })
export class BreakpointService implements OnDestroy {
  private readonly matchesMap = signal<Record<TailwindBreakpoint, boolean>>({
    base: true,
    sm: false,
    md: false,
    lg: false,
    xl: false,
    '2xl': false,
  });

  private mediaQueries: MediaQueryList[] = [];
  private listeners: Array<() => void> = [];

  constructor() {
    this.setupListeners();
  }

  private setupListeners() {
    this.mediaQueries = TAILWIND_BREAKPOINTS.map(bp => window.matchMedia(bp.query));

    const update = () => {
      const current: Record<TailwindBreakpoint, boolean> = {
        base: true,
        sm: false,
        md: false,
        lg: false,
        xl: false,
        '2xl': false,
      };

      for (const bp of TAILWIND_BREAKPOINTS) {
        const mq = window.matchMedia(bp.query);
        if (mq.matches) {
          current[bp.name] = true;
        }
      }

      const anyOther = TAILWIND_BREAKPOINTS.some(bp => current[bp.name]);
      current.base = !anyOther;

      this.matchesMap.set(current);
    };

    for (const mq of this.mediaQueries) {
      const listener = () => update();
      mq.addEventListener('change', listener);
      this.listeners.push(() => mq.removeEventListener('change', listener));
    }

    update();
  }

  ngOnDestroy() {
    for (const dispose of this.listeners) {
      dispose();
    }
  }

  readonly matches = computed(() => this.matchesMap());

  readonly current = computed<TailwindBreakpoint>(() => {
    const m = this.matchesMap();
    if (m['2xl']) return '2xl';
    if (m['xl']) return 'xl';
    if (m['lg']) return 'lg';
    if (m['md']) return 'md';
    if (m['sm']) return 'sm';
    return 'base';
  });

  isUp(breakpoint: TailwindBreakpoint) {
    return computed(() => {
      const m = this.matchesMap();
      switch (breakpoint) {
        case 'base':
          return true;
        case 'sm':
          return m.sm || m.md || m.lg || m.xl || m['2xl'];
        case 'md':
          return m.md || m.lg || m.xl || m['2xl'];
        case 'lg':
          return m.lg || m.xl || m['2xl'];
        case 'xl':
          return m.xl || m['2xl'];
        case '2xl':
          return m['2xl'];
      }
    });
  }

  isDown(breakpoint: TailwindBreakpoint) {
    return computed(() => {
      const m = this.matchesMap();
      switch (breakpoint) {
        case '2xl':
          return true;
        case 'xl':
          return m.base || m.sm || m.md || m.lg || m.xl;
        case 'lg':
          return m.base || m.sm || m.md || m.lg;
        case 'md':
          return m.base || m.sm || m.md;
        case 'sm':
          return m.base || m.sm;
        case 'base':
          return m.base;
      }
    });
  }
}
