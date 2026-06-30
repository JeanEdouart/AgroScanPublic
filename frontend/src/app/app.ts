import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { ConfirmationService } from './core/confirmation.service';
import { ConfirmationDialog } from './shared/confirmation-dialog/confirmation-dialog';
import { NotificationCenter } from './shared/notification-center/notification-center';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, ConfirmationDialog, NotificationCenter],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  private readonly confirmation = inject(ConfirmationService);
  protected readonly menuOpen = signal(false);

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected async logout(): Promise<void> {
    const confirmed = await this.confirmation.confirm({
      title: 'Se déconnecter ?',
      message: 'Votre session locale sera fermée. Vous devrez vous reconnecter pour accéder à votre espace.',
      confirmLabel: 'Se déconnecter',
      tone: 'danger',
    });
    if (!confirmed) return;
    this.auth.logout();
    this.closeMenu();
  }
}
