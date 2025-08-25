import { HttpErrorResponse } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';

import { AxiosError } from 'axios';

import { KratosMessage } from '../models/kratos/kratos-message';
import { KratosNode } from '../models/kratos/kratos-node';

export function translateKratosError(error: unknown, translateService: TranslateService): string | null {
  const ui = extractKratosUi(error);
  if (!ui) return null;

  const messages: string[] = [];

  if (ui?.messages) {
    messages.push(...translateKratosMessages(ui.messages, translateService));
  }

  const nodes: KratosNode[] | undefined = ui?.nodes;
  if (nodes && nodes.length > 0) {
    messages.push(...translateKratosNodeMessages(nodes, translateService));
  }

  return messages.length > 0 ? messages.join('\n') : null;
}

function extractKratosUi(error: unknown): any | undefined {
  if (error instanceof HttpErrorResponse) {
    return (error as any)?.error?.ui;
  }

  if (error instanceof AxiosError) {
    return error?.response?.data?.ui;
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
