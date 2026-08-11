import '@angular/compiler';

import { describe, expect, it, vi } from 'vitest';

import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { FinancesTitleBarExtraButton } from '../../components/finances-title-bar/finances-title-bar.component';
import { OverviewGroupPageComponent } from './overview-group-page.component';

const userId = '00000000-0000-0000-0000-000000000001';
const otherUserId = '00000000-0000-0000-0000-000000000002';

describe('OverviewGroupPageComponent ownership and departure', () => {
  it('keeps viewer permissions when a group name event arrives', () => {
    const component = createComponent(group(false, 'VIEWER', []));

    component.groupUpdated({ id: component.group.id, name: 'Renamed' });

    expect(component.group.name).toBe('Renamed');
    expect(component.group.permissions).toEqual([]);
    expect(component.extraButtons.some((button: FinancesTitleBarExtraButton) => button.tooltip === 'general.edit')).toBe(false);
  });

  it('shows leave only to a non-owner and sends the request only after confirmation', async () => {
    const component = createComponent(group(false));
    expect(component.extraButtons.some((button: FinancesTitleBarExtraButton) => button.severity === 'danger')).toBe(true);

    component.askForConfirmationToLeave();
    expect(component.groupService.leaveGroup).not.toHaveBeenCalled();
    await component.confirmation.accept();
    expect(component.groupService.leaveGroup).toHaveBeenCalledWith(component.group.id);

    const owner = createComponent(group(true));
    expect(owner.extraButtons.some((button: FinancesTitleBarExtraButton) => button.severity === 'danger')).toBe(false);
  });

  it('refetches for every member so ownership can update the tier and limits without a reload', async () => {
    const promoted = createComponent(group(false));
    await promoted.ownershipChanged();
    expect(promoted.groupService.getGroup).toHaveBeenCalledTimes(1);
    expect(promoted.group.tier).toBe('PRO');

    const previousOwner = createComponent(group(true));
    await previousOwner.ownershipChanged();
    expect(previousOwner.groupService.getGroup).toHaveBeenCalledTimes(1);
    expect(previousOwner.group.tier).toBe('PRO');
  });
});

function createComponent(initialGroup: GroupWithRoleDto): any {
  const component = Object.create(OverviewGroupPageComponent.prototype) as any;
  component.group = initialGroup;
  component.groupId = initialGroup.id;
  component.dashboard = { currency: 'BRL', debtPairs: [] };
  component.userService = { user: () => ({ id: userId }) };
  component.groupService = {
    getGroup: vi.fn().mockResolvedValue({ ...initialGroup, ownerUserId: userId, isOwner: true, role: 'ADMIN', tier: 'PRO' }),
    leaveGroup: vi.fn().mockResolvedValue(undefined),
  };
  component.router = { navigate: vi.fn().mockResolvedValue(true) };
  component.errorMessageService = { handleError: vi.fn() };
  component.messageService = {};
  component.translateService = { instant: (key: string) => key };
  component.confirmationService = {
    confirm: vi.fn((confirmation: unknown) => {
      component.confirmation = confirmation;
    }),
  };
  component.extraButtons = component.createExtraButtons();
  return component;
}

function group(
  isOwner: boolean,
  role: GroupWithRoleDto['role'] = 'ADMIN',
  permissions: GroupWithRoleDto['permissions'] = ['EDIT_GROUP'],
): GroupWithRoleDto {
  return {
    tier: 'COMMON',
    id: '10000000-0000-0000-0000-000000000001',
    name: 'Shared',
    ownerUserId: isOwner ? userId : otherUserId,
    isOwner,
    role,
    permissions,
  };
}
