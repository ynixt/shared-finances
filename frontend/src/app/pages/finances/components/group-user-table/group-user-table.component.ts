import { Component, ViewChild, computed, effect, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCheck, faCrown, faEdit, faRightLeft, faTimes } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Ripple } from 'primeng/ripple';
import { Select } from 'primeng/select';
import { Table, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { UserAvatarComponent } from '../../../../components/user-avatar/user-avatar.component';
import { GroupUserDto, GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import {
  GroupPermissions,
  GroupPermissions__Obj,
  UserGroupRole,
  UserGroupRole__Options,
} from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { I18nOption } from '../../../../models/i18n-option';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { UserService } from '../../../../services/user.service';
import { DEFAULT_ERROR_LIFE } from '../../../../util/error-util';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { GroupService } from '../../services/group.service';

type GroupUserWithIdDto = GroupUserDto & { id: string };

@Component({
  selector: 'app-group-user-table',
  imports: [
    TableModule,
    TranslatePipe,
    UserAvatarComponent,
    ButtonDirective,
    Ripple,
    Tag,
    Select,
    FormsModule,
    FaIconComponent,
    ToggleSwitch,
    ConfirmDialog,
  ],
  templateUrl: './group-user-table.component.html',
  styleUrl: './group-user-table.component.scss',
  providers: [ConfirmationService],
})
export class GroupUserTableComponent {
  loading = false;
  members: GroupUserWithIdDto[] = [];
  clonedMembersById: { [s: string]: GroupUserWithIdDto } = {};
  roles: I18nOption<UserGroupRole>[];
  submitting = false;
  updatingPlanningOptIn = new Set<string>();

  group = input<GroupWithRoleDto | undefined>(undefined);
  groupRole = signal<UserGroupRole>('VIEWER');
  hasPermissionToModifyRoles = computed<boolean>(() => this.group()?.permissions?.includes(GroupPermissions__Obj.CHANGE_ROLE) === true);

  @ViewChild('table') table: Table | undefined = undefined;

  constructor(
    private groupService: GroupService,
    private translateService: TranslateService,
    private messageService: MessageService,
    public userService: UserService,
    private confirmationService: ConfirmationService,
    private errorMessageService: ErrorMessageService,
  ) {
    this.roles = UserGroupRole__Options.map(value => ({
      value,
      label: this.translateService.instant('enums.userGroupRole.' + value),
    }));

    effect(async () => {
      this.loading = true;

      const groupId = this.group()?.id;
      const user = this.userService.user();

      if (groupId == null || user == null) {
        this.members = [];
      } else {
        this.members = (await this.groupService.findAllMembers(groupId)).map(groupUser => ({
          ...groupUser,
          id: `${groupId}-${groupUser.user.id}`,
        }));

        this.groupRole.set(this.members.find(m => m.user.id === user.id)!!.role);
      }

      this.loading = false;
    });
  }

  onRowEditInit(groupUser: GroupUserWithIdDto) {
    this.clonedMembersById[groupUser.id] = { ...groupUser };
  }

  onRowEditCancel(groupUser: GroupUserWithIdDto, index: number) {
    this.members[index] = this.clonedMembersById[groupUser.id as string];
    delete this.clonedMembersById[groupUser.id as string];
    this.table?.cancelRowEdit(groupUser);
  }

  async onRowEditSave(groupUser: GroupUserWithIdDto, rowElement: HTMLTableRowElement) {
    if (this.submitting) return;

    try {
      this.submitting = true;
      await this.groupService.updateMemberRole(this.group()!!.id, groupUser.user.id, groupUser.role);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.manageTeamPage.successChangeMessage'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      delete this.clonedMembersById[groupUser.id as string];
      this.table?.saveRowEdit(groupUser, rowElement);
    } catch (err) {
      console.error(err);
      this.errorMessageService.handleError(err, this.messageService);
    } finally {
      this.submitting = false;
    }
  }

  askForOwnershipTransfer(groupUser: GroupUserWithIdDto) {
    const destinationName = this.fullName(groupUser);
    this.confirmationService.confirm({
      message: this.translateService.instant('financesPage.groupsPage.manageTeamPage.transferConfirmation', { name: destinationName }),
      header: this.translateService.instant('financesPage.groupsPage.manageTeamPage.transferOwnership'),
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('financesPage.groupsPage.manageTeamPage.transferOwnership'),
      rejectLabel: this.translateService.instant('general.cancel'),
      accept: () => this.transferOwnership(groupUser),
    });
  }

  isGroupOwner(groupUser: GroupUserWithIdDto): boolean {
    return this.group()?.ownerUserId === groupUser.user.id;
  }

  canTransferOwnership(groupUser: GroupUserWithIdDto): boolean {
    return this.group()?.isOwner === true && !this.isOwnUser(groupUser);
  }

  currentOwnerName(): string {
    const owner = this.members.find(member => this.isGroupOwner(member));
    return owner == null ? '' : this.fullName(owner);
  }

  private fullName(groupUser: GroupUserWithIdDto): string {
    return this.translateService.instant('name.fullName', {
      firstName: groupUser.user.firstName,
      lastName: groupUser.user.lastName,
    });
  }

  private async transferOwnership(groupUser: GroupUserWithIdDto) {
    const group = this.group();
    if (group == null || !group.isOwner || this.isOwnUser(groupUser) || this.submitting) return;

    this.submitting = true;
    try {
      const updated = await this.groupService.transferOwnership(group.id, groupUser.user.id);
      group.ownerUserId = updated.ownerUserId;
      group.isOwner = updated.isOwner;
      groupUser.role = 'ADMIN';
      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.manageTeamPage.transferSuccess', { name: this.fullName(groupUser) }),
        life: DEFAULT_SUCCESS_LIFE,
      });
    } catch (err) {
      this.errorMessageService.handleError(err, this.messageService);
    } finally {
      this.submitting = false;
    }
  }

  isOwnUser(groupUser: GroupUserWithIdDto): boolean {
    return this.userService.user()?.id === groupUser.user.id;
  }

  isUpdatingPlanningOptIn(groupUserId: string): boolean {
    return this.updatingPlanningOptIn.has(groupUserId);
  }

  async onPlanningOptInChange(groupUser: GroupUserWithIdDto, allowPlanningSimulator: boolean) {
    const groupId = this.group()?.id;
    if (groupId == null || !this.isOwnUser(groupUser) || this.isUpdatingPlanningOptIn(groupUser.id)) return;

    const oldValue = groupUser.allowPlanningSimulator;
    groupUser.allowPlanningSimulator = allowPlanningSimulator;
    this.updatingPlanningOptIn.add(groupUser.id);

    try {
      await this.groupService.updateOwnPlanningSimulatorOptIn(groupId, allowPlanningSimulator);
      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.manageTeamPage.planningOptInSaved'),
        life: DEFAULT_SUCCESS_LIFE,
      });
    } catch (err) {
      groupUser.allowPlanningSimulator = oldValue;
      console.error(err);
      this.messageService.add({
        severity: 'error',
        summary: this.translateService.instant('error.genericTitle'),
        detail: this.translateService.instant('error.genericMessage'),
        life: DEFAULT_ERROR_LIFE,
      });
    } finally {
      this.updatingPlanningOptIn.delete(groupUser.id);
    }
  }

  getSeverityForRole(role: UserGroupRole): 'success' | 'warn' | 'danger' {
    switch (role) {
      case 'ADMIN':
        return 'danger';
      case 'EDITOR':
        return 'warn';
      case 'VIEWER':
        return 'success';
    }
  }

  protected readonly faEdit = faEdit;
  protected readonly faCrown = faCrown;
  protected readonly faRightLeft = faRightLeft;
  protected readonly faCheck = faCheck;
  protected readonly faTimes = faTimes;
}
