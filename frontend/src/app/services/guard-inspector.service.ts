import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class GuardInspector {
  constructor(private router: Router) {}

  hasCanActivateInHierarchy(guard: CanActivateFn): boolean {
    const leaf = getPrimaryLeaf(this.router.routerState.snapshot.root);

    return leaf.pathFromRoot.some(s => {
      const cfg = s.routeConfig;
      const list = cfg?.canActivate ?? [];
      return list.includes(guard);
    });
  }
}

function getPrimaryLeaf(root: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
  let node: ActivatedRouteSnapshot = root;

  while (true) {
    const primaryChild = node.children.find(c => c.outlet === 'primary');
    if (!primaryChild) return node;
    node = primaryChild;
  }
}
