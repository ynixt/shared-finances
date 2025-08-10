import { HttpErrorResponse } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';

import { KratosMessage } from '../models/kratos/kratos-message';
import { KratosNode } from '../models/kratos/kratos-node';

export function translateKratosError(error: unknown, translateService: TranslateService): string | null {
  if (error instanceof HttpErrorResponse) {
    const ui = (error as any)?.error?.ui;
    let messages: string[] = [];

    if (ui?.messages) {
      messages = [...messages, ...translateKratosMessages(ui.messages, translateService)];
    }

    const nodes: KratosNode[] | undefined = (error as any)?.error?.ui?.nodes;

    if (nodes && nodes.length > 0) {
      messages = [...messages, ...translateKratosNodeMessages(nodes, translateService)];
    }

    if (messages.length > 0) {
      return messages.join('\n');
    }
  }

  return null;
}

export function translateKratosMessage(msg: KratosMessage, translateService: TranslateService): string {
  const key = `kratos.errors.${msg.id}`;

  const params = msg.context ?? {};

  if (params['property'] != null) {
    params['property'] = translateService.instant(`kratos.properties.${params['property']}`);
  }

  const translated = translateService.instant(key, params);

  if (translated === key) {
    // No translation found
    return msg.text;
  }

  return translated;
}

export function translateKratosMessages(msgs: KratosMessage[], translateService: TranslateService): string[] {
  const messages: string[] = [];

  msgs.forEach(message => {
    messages.push(translateKratosMessage(message, translateService));
  });

  return messages;
}

export function translateKratosNodeMessages(nodes: KratosNode[], translateService: TranslateService): string[] {
  const messages: string[] = [];

  nodes.forEach(node => {
    if (node.messages && node.messages.length > 0) {
      node.messages.forEach(message => {
        messages.push(translateKratosMessage(message, translateService));
      });
    }
  });

  return messages;
}
