import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AdminUser, AdminUserSearch, AdminUserService, UserRole } from '../../core/admin-user.service';
import { ApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { ConfirmationService } from '../../core/confirmation.service';
import { PageResponse } from '../../core/scan.service';

@Component({
  selector: 'app-admin-users',
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
})
export class AdminUsers {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly userService = inject(AdminUserService);
  private readonly confirmation = inject(ConfirmationService);
  protected readonly auth = inject(AuthService);
  private readonly editDialog = viewChild.required<ElementRef<HTMLDialogElement>>('editDialog');
  private readonly deleteDialog = viewChild.required<ElementRef<HTMLDialogElement>>('deleteDialog');

  protected readonly results = signal<PageResponse<AdminUser> | null>(null);
  protected readonly selectedUser = signal<AdminUser | null>(null);
  protected readonly loading = signal(true);
  protected readonly actionLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly dialogError = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly ascending = signal(true);
  protected readonly sortBy = signal<'firstName' | 'lastName' | 'email' | 'role' | 'createdAt'>('lastName');

  protected readonly searchForm = this.fb.group({ search: [''] });
  protected readonly editForm = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    role: this.fb.control<UserRole>('USER', Validators.required),
    enabled: [true],
  });

  constructor() {
    this.load();
  }

  protected search(): void {
    this.page.set(0);
    this.load();
  }

  protected clearSearch(): void {
    this.searchForm.reset();
    this.page.set(0);
    this.load();
  }

  protected changeSort(event: Event): void {
    this.sortBy.set((event.target as HTMLSelectElement).value as AdminUserSearch['sortBy']);
    this.page.set(0);
    this.load();
  }

  protected toggleSortDirection(): void {
    this.ascending.update((value) => !value);
    this.page.set(0);
    this.load();
  }

  protected previousPage(): void {
    if (this.page() > 0) {
      this.page.update((value) => value - 1);
      this.load();
    }
  }

  protected nextPage(): void {
    const results = this.results();
    if (results && this.page() + 1 < results.totalPages) {
      this.page.update((value) => value + 1);
      this.load();
    }
  }

  protected isCurrentUser(user: AdminUser): boolean {
    return user.id === this.auth.user()?.id;
  }

  protected openEdit(user: AdminUser): void {
    if (this.isCurrentUser(user)) return;
    this.selectedUser.set(user);
    this.dialogError.set(null);
    this.editForm.setValue({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role,
      enabled: user.enabled,
    });
    this.editDialog().nativeElement.showModal();
  }

  protected closeEdit(): void {
    this.editDialog().nativeElement.close();
    this.selectedUser.set(null);
  }

  protected async updateUser(): Promise<void> {
    const user = this.selectedUser();
    if (!user || this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    const confirmed = await this.confirmation.confirm({
      title: 'Modifier ce compte ?',
      message: `Les informations et les droits de ${user.firstName} ${user.lastName} seront mis à jour.`,
      confirmLabel: 'Enregistrer',
    });
    if (!confirmed) return;
    this.actionLoading.set(true);
    this.dialogError.set(null);
    this.userService.update(user.id, this.editForm.getRawValue())
      .pipe(finalize(() => this.actionLoading.set(false)))
      .subscribe({
        next: () => {
          this.closeEdit();
          this.load();
        },
        error: (error: HttpErrorResponse) => this.dialogError.set(this.errorMessage(error)),
      });
  }

  protected openDelete(user: AdminUser): void {
    if (this.isCurrentUser(user)) return;
    this.selectedUser.set(user);
    this.dialogError.set(null);
    this.deleteDialog().nativeElement.showModal();
  }

  protected closeDelete(): void {
    this.deleteDialog().nativeElement.close();
    this.selectedUser.set(null);
  }

  protected deleteUser(): void {
    const user = this.selectedUser();
    if (!user) return;
    this.actionLoading.set(true);
    this.dialogError.set(null);
    this.userService.delete(user.id)
      .pipe(finalize(() => this.actionLoading.set(false)))
      .subscribe({
        next: () => {
          this.closeDelete();
          if (this.results()?.content.length === 1 && this.page() > 0) this.page.update((value) => value - 1);
          this.load();
        },
        error: (error: HttpErrorResponse) => this.dialogError.set(this.errorMessage(error)),
      });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.userService.search({
      search: this.searchForm.controls.search.value.trim(),
      page: this.page(),
      size: 10,
      sortBy: this.sortBy(),
      ascending: this.ascending(),
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (results) => this.results.set(results),
      error: (error: HttpErrorResponse) => this.error.set(this.errorMessage(error)),
    });
  }

  private errorMessage(error: HttpErrorResponse): string {
    return (error.error as ApiError | undefined)?.message ?? 'Une erreur est survenue. Veuillez réessayer.';
  }
}
