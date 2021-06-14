import { createAction, props } from '@ngrx/store';
import { CreditCard } from 'src/app/@core/models';

export const getCreditCards = createAction('[CreditCard] GET_CREDIT_CARDS');
export const getCreditCardsError = createAction('[CreditCard] GET_CREDIT_CARDS_ERROR', props<{ error: any }>());
export const getCreditCardsSuccess = createAction('[CreditCard] GET_CREDIT_CARDS_SUCCESS', props<{ creditCards: CreditCard[] }>());

export const creditCardRemoved = createAction('[CreditCard] CRDIT_CARD_REMOVED', props<{ creditCardId: string }>());
export const creditCardAdded = createAction('[CreditCard] CRDIT_CARD_ADDED', props<{ newCreditCard: CreditCard }>());
export const creditCardChanged = createAction('[CreditCard] CRDIT_CARD_CHANGED', props<{ updatedCreditCard: CreditCard }>());
