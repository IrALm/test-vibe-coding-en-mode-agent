import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Alert } from '../../shared/ui/alert/alert';

@Component({
  selector: 'app-email-verification-required',
  imports: [RouterLink, Alert],
  templateUrl: './email-verification-required.html'
})
export class EmailVerificationRequired {
  private readonly route = inject(ActivatedRoute);

  readonly email = this.route.snapshot.queryParamMap.get('email') ?? '';
}
