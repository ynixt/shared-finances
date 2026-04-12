import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { describe, expect, it } from 'vitest';

import { resolveGoalWorkspaceContext } from './goal-workspace-context';

function routeWithParams(pathParams: Array<Record<string, string | undefined>>): ActivatedRoute {
  return {
    snapshot: {
      pathFromRoot: pathParams.map(params => ({ paramMap: convertToParamMap(params) })),
    },
  } as unknown as ActivatedRoute;
}

describe('resolveGoalWorkspaceContext', () => {
  it('returns individual workspace when route tree has no groupId', () => {
    const route = routeWithParams([{}, { id: 'goal-1' }]);

    const context = resolveGoalWorkspaceContext(route);

    expect(context.scope).toBe('individual');
    expect(context.groupId).toBeNull();
    expect(context.goalsRoot).toEqual(['/app/goals']);
  });

  it('returns group workspace when route tree has groupId', () => {
    const route = routeWithParams([{}, { groupId: 'group-42' }, { id: 'goal-1' }]);

    const context = resolveGoalWorkspaceContext(route);

    expect(context.scope).toBe('group');
    expect(context.groupId).toBe('group-42');
    expect(context.goalsRoot).toEqual(['/app/groups', 'group-42', 'goals']);
  });
});
