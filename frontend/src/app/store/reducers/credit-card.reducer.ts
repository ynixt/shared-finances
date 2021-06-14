import { createReducer, Action, on, ActionReducer } from '@ngrx/store';
import { CreditCard } from 'src/app/@core/models';
import { CreditCardActions } from '../actions';

export type CreditCardState = {
  loading: boolean;
  error?: any;
  creditCards?: CreditCard[];
  done: boolean;
};

export const initialState: CreditCardState = {
  loading: false,
  done: false,
};

const creditCardReducer: ActionReducer<CreditCardState, Action> = createReducer(
  initialState,
  on(CreditCardActions.creditCardAdded, (state, action) => {
    const creditCards = [...state.creditCards, action.newCreditCard];

    return {
      ...initialState,
      loading: true,
      creditCards,
    };
  }),
  on(CreditCardActions.creditCardChanged, (state, action) => {
    const creditCards = [
      ...state.creditCards.filter(creditCard => creditCard.id !== action.updatedCreditCard.id),
      action.updatedCreditCard,
    ];

    return {
      ...initialState,
      loading: true,
      creditCards,
    };
  }),
  on(CreditCardActions.creditCardRemoved, (state, action) => {
    const creditCards = state.creditCards.filter(creditCard => creditCard.id !== action.creditCardId);

    return {
      ...initialState,
      loading: true,
      creditCards,
    };
  }),
  on(CreditCardActions.getCreditCards, () => {
    return {
      ...initialState,
      loading: true,
    };
  }),
  on(CreditCardActions.getCreditCardsSuccess, (_, action) => {
    return {
      ...initialState,
      creditCards: action.creditCards,
      done: true,
    };
  }),
  on(CreditCardActions.getCreditCardsError, (_, action) => {
    return {
      ...initialState,
      error: action.error,
      done: true,
    };
  }),
);

export function reducer(state: CreditCardState, action: Action): CreditCardState {
  return creditCardReducer(state, action);
}
