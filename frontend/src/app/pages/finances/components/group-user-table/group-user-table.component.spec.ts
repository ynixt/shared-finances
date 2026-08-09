import '@angular/compiler';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { describe, expect, it, vi } from 'vitest';

import { GroupUserDto, GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { UserService } from '../../../../services/user.service';
import { EditGroupPageComponent } from '../../groups-page/edit-group-page/edit-group-page.component';
import { GroupService } from '../../services/group.service';
import { GroupUserTableComponent } from './group-user-table.component';

const ownerId = '00000000-0000-0000-0000-000000000001';
const memberId = '00000000-0000-0000-0000-000000000002';

const ownerGroup = (isOwner = true): GroupWithRoleDto => ({
  id: '10000000-0000-0000-0000-000000000001',
  name: 'Shared',
  ownerUserId: ownerId,
  isOwner,
  role: 'ADMIN',
  permissions: ['CHANGE_ROLE', 'DELETE_GROUP'],
});

const member: GroupUserDto & { id: string } = {
  id: `group-${memberId}`,
  user: { id: memberId, email: 'member@example.com', firstName: 'Member', lastName: 'User', photoUrl: null },
  role: 'VIEWER',
  allowPlanningSimulator: true,
};

const ownerMember: GroupUserDto & { id: string } = {
  id: `group-${ownerId}`,
  user: { id: ownerId, email: 'owner@example.com', firstName: 'Owner', lastName: 'User', photoUrl: null },
  role: 'ADMIN',
  allowPlanningSimulator: true,
};

describe('Group ownership controls', () => {
  it('sends a transfer only after confirmation and promotes the displayed destination', async () => {
    const confirmationService = { confirm: vi.fn() };
    const groupService = {
      findAllMembers: vi.fn().mockResolvedValue([member]),
      transferOwnership: vi.fn().mockResolvedValue({ ...ownerGroup(false), ownerUserId: memberId }),
    };
    const fixture = createTableFixture(confirmationService, groupService, ownerGroup());
    const component = fixture.componentInstance;

    component.askForOwnershipTransfer(member);
    expect(groupService.transferOwnership).not.toHaveBeenCalled();

    const confirmation = confirmationService.confirm.mock.calls[0][0];
    await confirmation.accept();

    expect(groupService.transferOwnership).toHaveBeenCalledWith(ownerGroup().id, memberId);
    expect(member.role).toBe('ADMIN');
  });

  it('does not send a transfer when confirmation is cancelled', () => {
    const confirmationService = { confirm: vi.fn() };
    const groupService = { findAllMembers: vi.fn().mockResolvedValue([member]), transferOwnership: vi.fn() };
    const component = createTableFixture(confirmationService, groupService, ownerGroup()).componentInstance;

    component.askForOwnershipTransfer(member);
    const confirmation = confirmationService.confirm.mock.calls[0][0];
    confirmation.reject?.();

    expect(groupService.transferOwnership).not.toHaveBeenCalled();
  });

  it('shows transfer and delete capabilities only to the owner', () => {
    const confirmationService = { confirm: vi.fn() };
    const groupService = { findAllMembers: vi.fn().mockResolvedValue([member]), transferOwnership: vi.fn() };
    const ownerComponent = createTableFixture(confirmationService, groupService, ownerGroup()).componentInstance;
    const nonOwnerComponent = createTableFixture(confirmationService, groupService, ownerGroup(false)).componentInstance;

    expect(ownerComponent.canTransferOwnership(member)).toBe(true);
    expect(nonOwnerComponent.canTransferOwnership(member)).toBe(false);

    const editPage = Object.create(EditGroupPageComponent.prototype) as EditGroupPageComponent;
    editPage.group = ownerGroup();
    expect(editPage.canDeleteGroup()).toBe(true);
    editPage.group = ownerGroup(false);
    expect(editPage.canDeleteGroup()).toBe(false);
  });
});

function createTableFixture(confirmationService: { confirm: ReturnType<typeof vi.fn> }, groupService: object, group: GroupWithRoleDto) {
  const suppliedGroupService = groupService as { findAllMembers?: ReturnType<typeof vi.fn> };
  suppliedGroupService.findAllMembers?.mockResolvedValue([ownerMember, member]);
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [GroupUserTableComponent],
    providers: [
      { provide: GroupService, useValue: groupService },
      { provide: UserService, useValue: { user: signal({ id: ownerId }) } },
      { provide: TranslateService, useValue: { instant: (key: string, args?: { name?: string }) => args?.name ?? key } },
      { provide: MessageService, useValue: { add: vi.fn() } },
      { provide: ErrorMessageService, useValue: { handleError: vi.fn() } },
    ],
  });
  TestBed.overrideComponent(GroupUserTableComponent, {
    set: {
      template: '',
      providers: [{ provide: ConfirmationService, useValue: confirmationService }],
    },
  });
  const fixture = TestBed.createComponent(GroupUserTableComponent);
  fixture.componentRef.setInput('group', group);
  fixture.detectChanges();
  return fixture;
}
