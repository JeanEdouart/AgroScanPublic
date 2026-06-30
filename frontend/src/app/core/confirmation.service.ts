import { Injectable, signal } from '@angular/core';

export interface ConfirmationOptions {
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel?: string;
  tone?: 'default' | 'danger';
}

export interface ConfirmationRequest extends Required<ConfirmationOptions> {
  resolve: (confirmed: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  readonly request = signal<ConfirmationRequest | null>(null);

  confirm(options: ConfirmationOptions): Promise<boolean> {
    return new Promise((resolve) => {
      this.request.set({
        cancelLabel: 'Annuler',
        tone: 'default',
        ...options,
        resolve,
      });
    });
  }

  answer(confirmed: boolean): void {
    const current = this.request();
    if (!current) return;
    this.request.set(null);
    current.resolve(confirmed);
  }
}
