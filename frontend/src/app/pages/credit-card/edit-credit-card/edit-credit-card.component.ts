import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HotToastService } from '@ngneat/hot-toast';
import { CreditCard } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { CreditCardService } from '../credit-card.service';

@Component({
  selector: 'app-edit-credit-card',
  templateUrl: './edit-credit-card.component.html',
  styleUrls: ['./edit-credit-card.component.scss'],
})
export class EditCreditCardComponent implements OnInit {
  creditCard: CreditCard;

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private creditCardService: CreditCardService,
    private errorService: ErrorService,
    private toast: HotToastService,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.params.subscribe(params => this.getCreditCard(params.id));
  }

  private async getCreditCard(creditCardId: string) {
    try {
      const creditCard = await this.creditCardService.getById(creditCardId);

      if (!creditCard) {
        this.router.navigateByUrl('/404');
      }

      this.creditCard = creditCard;
    } catch (err) {
      this.toast.error(this.errorService.getInstantErrorMessage(err, 'generic-error', 'generic-error-with-description'));
    }
  }
}
