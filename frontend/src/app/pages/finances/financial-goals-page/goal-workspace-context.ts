import { ActivatedRoute } from '@angular/router';

export type GoalWorkspaceScope = 'individual' | 'group';

export interface GoalWorkspaceContext {
  scope: GoalWorkspaceScope;
  groupId: string | null;
  goalsRoot: string[];
}

export function resolveGoalWorkspaceContext(route: ActivatedRoute): GoalWorkspaceContext {
  const groupId = findParamFromRouteTree(route, 'groupId');
  if (groupId != null && groupId !== '') {
    return {
      scope: 'group',
      groupId,
      goalsRoot: ['/app/groups', groupId, 'goals'],
    };
  }
  return {
    scope: 'individual',
    groupId: null,
    goalsRoot: ['/app/goals'],
  };
}

function findParamFromRouteTree(route: ActivatedRoute, key: string): string | null {
  const pathFromRoot = route.snapshot.pathFromRoot;
  for (let i = pathFromRoot.length - 1; i >= 0; i -= 1) {
    const value = pathFromRoot[i].paramMap.get(key);
    if (value != null && value !== '') {
      return value;
    }
  }
  return null;
}
