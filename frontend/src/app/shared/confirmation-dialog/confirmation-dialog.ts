import { Component, ElementRef, effect, inject, viewChild } from '@angular/core';
import { ConfirmationService } from '../../core/confirmation.service';

@Component({
  selector: 'app-confirmation-dialog',
  templateUrl: './confirmation-dialog.html',
  styleUrl: './confirmation-dialog.scss',
})
export class ConfirmationDialog {
  protected readonly confirmation = inject(ConfirmationService);
  private readonly dialog = viewChild<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    effect(() => {
      const request = this.confirmation.request();
      const dialog = this.dialog()?.nativeElement;
      if (!dialog) return;
      if (request && !dialog.open) {
        dialog.showModal();
      } else if (!request && dialog.open) {
        dialog.close();
      }
    });
  }

  protected closeFromBackdrop(event: MouseEvent): void {
    if (event.target === this.dialog()?.nativeElement) {
      this.confirmation.answer(false);
    }
  }
}
