import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { InviteService } from './invite.service';

@Component({
  selector: 'app-invite',
  templateUrl: './invite.component.html',
  styleUrls: ['./invite.component.scss'],
})
export class InviteComponent implements OnInit, OnDestroy {
  private activatedRouteSubscription: Subscription;

  constructor(private activatedRoute: ActivatedRoute, private inviteService: InviteService, private router: Router) {}

  ngOnInit(): void {
    this.activatedRouteSubscription = this.activatedRoute.params.subscribe(params => this.loadInvite(params.inviteId));
  }

  ngOnDestroy(): void {
    if (this.activatedRouteSubscription) {
      this.activatedRouteSubscription.unsubscribe();
    }
  }

  private async loadInvite(inviteId: string): Promise<void> {
    const groupId = await this.inviteService.useInvite(inviteId);

    if (groupId != null) {
      this.goToGroup(groupId);
    } else {
      this.goToNotFound();
    }
  }

  private goToGroup(groupId: string): void {
    this.router.navigateByUrl(`/finances/shared/${groupId}`);
  }

  private goToNotFound(): void {
    this.router.navigateByUrl('/404');
  }
}
