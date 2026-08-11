import { HttpErrorResponse } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { describe, expect, it, vi } from 'vitest';

import { ErrorMessageService } from './error-message.service';

describe('ErrorMessageService plan quota refusals', () => {
  it.each([
    [undefined, 'planQuota.refusal.own'],
    ['another-user', 'planQuota.refusal.other'],
  ])('uses the distinct localized refusal when quota owner is %s', (quotaOwnerUserId, expectedKey) => {
    const translate = {
      instant: vi.fn((key: string, args?: Record<string, string>) =>
        key.startsWith('planQuota.refusal') ? `${key}:${args?.['quota']}` : 'Bank accounts',
      ),
    };
    const messages = { add: vi.fn() };
    const service = new ErrorMessageService(translate as unknown as TranslateService);

    service.handleError(
      new HttpErrorResponse({
        status: 409,
        error: {
          errorCode: 'PLAN_QUOTA_EXCEEDED',
          argsI18n: { quota: 'BANK_ACCOUNTS', ...(quotaOwnerUserId == null ? {} : { quotaOwnerUserId }) },
        },
      }),
      messages as unknown as MessageService,
    );

    expect(messages.add).toHaveBeenCalledWith(expect.objectContaining({ detail: `${expectedKey}:Bank accounts` }));
  });

  it('uses the group refusal when a group id is present', () => {
    const translate = { instant: vi.fn((key: string) => key) };
    const messages = { add: vi.fn() };
    new ErrorMessageService(translate as unknown as TranslateService).handleError(
      new HttpErrorResponse({
        status: 409,
        error: { errorCode: 'PLAN_QUOTA_EXCEEDED', argsI18n: { quota: 'GROUP_GOALS', groupId: 'group-1' } },
      }),
      messages as unknown as MessageService,
    );
    expect(messages.add).toHaveBeenCalledWith(expect.objectContaining({ detail: 'planQuota.refusal.group' }));
  });
});
