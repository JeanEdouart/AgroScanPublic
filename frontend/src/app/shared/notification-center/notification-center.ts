import { Component, ElementRef, HostListener, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { NotificationService } from '../../core/notification.service';

@Component({
  selector: 'app-notification-center',
  templateUrl: './notification-center.html',
  styleUrl: './notification-center.scss',
})
export class NotificationCenter {
  protected readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly element = inject(ElementRef<HTMLElement>);
  protected readonly open = signal(false);

  constructor() {
    effect(() => {
      if (this.auth.user()) this.notifications.connect();
      else this.notifications.disconnect();
    });
  }

  protected toggle(): void {
    this.open.update((value) => !value);
  }

  protected dismiss(id: string, event: MouseEvent): void {
    event.stopPropagation();
    this.notifications.dismiss(id);
  }

  protected clearFinished(): void {
    this.notifications.clearFinished();
  }

  protected openScan(scanId: number | null): void {
    if (scanId === null) return;
    this.open.set(false);
    void this.router.navigate(['/mes-scans'], { queryParams: { scanId } });
  }

  @HostListener('document:click', ['$event'])
  protected closeOnOutsideClick(event: MouseEvent): void {
    if (!this.element.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }
}
