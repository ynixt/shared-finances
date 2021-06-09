import { Component, OnInit } from '@angular/core';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { BankAccount, User } from 'src/app/@core/models';
import { AuthSelectors } from 'src/app/store/services/selectors';

@UntilDestroy()
@Component({
  selector: 'app-bank-account',
  templateUrl: './bank-account.component.html',
  styleUrls: ['./bank-account.component.scss'],
})
export class BankAccountComponent implements OnInit {
  bankAccounts: BankAccount[] = [];

  constructor(private authSelector: AuthSelectors) {}

  async ngOnInit(): Promise<void> {
    this.authSelector.user$.pipe(untilDestroyed(this)).subscribe(user => this.fillBankAccounts(user));
  }

  private fillBankAccounts(user: User): void {
    this.bankAccounts = [...(user.bankAccounts || [])].sort((bankAccountA, bankAccountB) =>
      bankAccountA.name.localeCompare(bankAccountB.name),
    );
  }
}
