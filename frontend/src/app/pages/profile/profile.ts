import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { ConfirmationService } from '../../core/confirmation.service';

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly confirmation = inject(ConfirmationService);
  private readonly currentUser = this.auth.user();

  protected readonly user = this.auth.user;
  protected readonly profileLoading = signal(false);
  protected readonly passwordLoading = signal(false);
  protected readonly photoLoading = signal(false);
  protected readonly profileMessage = signal<string | null>(null);
  protected readonly passwordMessage = signal<string | null>(null);
  protected readonly photoMessage = signal<string | null>(null);
  protected readonly profileError = signal<string | null>(null);
  protected readonly passwordError = signal<string | null>(null);
  protected readonly photoError = signal<string | null>(null);

  protected readonly profileForm = this.fb.group({
    firstName: [this.currentUser?.firstName ?? '', [Validators.required, Validators.maxLength(80)]],
    lastName: [this.currentUser?.lastName ?? '', [Validators.required, Validators.maxLength(80)]],
    email: [this.currentUser?.email ?? '', [Validators.required, Validators.email, Validators.maxLength(320)]],
  });

  protected readonly passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
    confirmation: ['', Validators.required],
  });

  constructor() {
    this.auth.loadCurrentUser().subscribe({
      next: (user) => this.profileForm.setValue({ firstName: user.firstName, lastName: user.lastName, email: user.email }),
      error: (error: HttpErrorResponse) => this.profileError.set(this.errorMessage(error)),
    });
  }

  protected async saveProfile(): Promise<void> {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      this.profileError.set('Vérifiez les informations saisies.');
      return;
    }
    const confirmed = await this.confirmation.confirm({
      title: 'Modifier votre profil ?',
      message: 'Votre nom, prénom ou adresse e-mail seront mis à jour.',
      confirmLabel: 'Enregistrer',
    });
    if (!confirmed) return;
    this.profileLoading.set(true);
    this.profileError.set(null);
    this.profileMessage.set(null);
    const value = this.profileForm.getRawValue();
    this.auth.updateProfile(value.firstName, value.lastName, value.email)
      .pipe(finalize(() => this.profileLoading.set(false)))
      .subscribe({
        next: () => {
          this.profileForm.markAsPristine();
          this.profileMessage.set('Vos informations ont été mises à jour.');
        },
        error: (error: HttpErrorResponse) => this.profileError.set(this.errorMessage(error)),
      });
  }

  protected async changePassword(): Promise<void> {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      this.passwordError.set('Vérifiez les mots de passe saisis.');
      return;
    }
    const value = this.passwordForm.getRawValue();
    if (value.newPassword !== value.confirmation) {
      this.passwordError.set('Les nouveaux mots de passe ne correspondent pas.');
      return;
    }
    const confirmed = await this.confirmation.confirm({
      title: 'Modifier le mot de passe ?',
      message: 'Toutes vos sessions seront déconnectées après cette modification.',
      confirmLabel: 'Modifier',
      tone: 'danger',
    });
    if (!confirmed) return;
    this.passwordLoading.set(true);
    this.passwordError.set(null);
    this.passwordMessage.set(null);
    this.auth.changePassword(value.currentPassword, value.newPassword)
      .pipe(finalize(() => this.passwordLoading.set(false)))
      .subscribe({
        next: () => {
          this.passwordMessage.set('Mot de passe modifié. Vous allez être redirigé vers la connexion.');
          this.passwordForm.reset();
          this.auth.clearSession();
          setTimeout(() => void this.router.navigateByUrl('/connexion'), 900);
        },
        error: (error: HttpErrorResponse) => this.passwordError.set(this.errorMessage(error)),
      });
  }

  protected async selectProfilePhoto(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    this.photoError.set(null);
    this.photoMessage.set(null);
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      this.photoError.set('Sélectionnez une image JPEG, PNG ou WebP.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.photoError.set('La photo source doit peser moins de 5 Mo.');
      return;
    }

    const confirmed = await this.confirmation.confirm({
      title: this.user()?.profilePhotoDataUrl ? 'Remplacer la photo ?' : 'Ajouter cette photo ?',
      message: "La photo de profil sera enregistrée et affichée dans l'en-tête de votre compte.",
      confirmLabel: this.user()?.profilePhotoDataUrl ? 'Remplacer' : 'Ajouter',
    });
    if (!confirmed) return;

    this.photoLoading.set(true);
    try {
      const dataUrl = await this.readFile(file);
      const photoBase64 = await this.createProfilePhoto(dataUrl);
      this.auth.updateProfilePhoto(photoBase64, 'image/jpeg')
        .pipe(finalize(() => this.photoLoading.set(false)))
        .subscribe({
          next: () => this.photoMessage.set('Photo de profil mise à jour.'),
          error: (error: HttpErrorResponse) => this.photoError.set(this.errorMessage(error)),
        });
    } catch {
      this.photoLoading.set(false);
      this.photoError.set("Impossible de lire cette image.");
    }
  }

  protected async removeProfilePhoto(): Promise<void> {
    const confirmed = await this.confirmation.confirm({
      title: 'Supprimer la photo ?',
      message: "L'icône de compte par défaut sera affichée à la place de votre photo.",
      confirmLabel: 'Supprimer',
      tone: 'danger',
    });
    if (!confirmed) return;
    this.photoLoading.set(true);
    this.photoError.set(null);
    this.photoMessage.set(null);
    this.auth.clearProfilePhoto()
      .pipe(finalize(() => this.photoLoading.set(false)))
      .subscribe({
        next: () => this.photoMessage.set('Photo de profil supprimée.'),
        error: (error: HttpErrorResponse) => this.photoError.set(this.errorMessage(error)),
      });
  }

  private errorMessage(error: HttpErrorResponse): string {
    return (error.error as ApiError | undefined)?.message ?? 'Une erreur est survenue. Veuillez réessayer.';
  }

  private readFile(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  private createProfilePhoto(dataUrl: string): Promise<string> {
    return new Promise((resolve, reject) => {
      const image = new Image();
      image.onload = () => {
        const size = 256;
        const canvas = document.createElement('canvas');
        canvas.width = size;
        canvas.height = size;
        const context = canvas.getContext('2d');
        if (!context) {
          reject();
          return;
        }
        const sourceSize = Math.min(image.width, image.height);
        const sourceX = (image.width - sourceSize) / 2;
        const sourceY = (image.height - sourceSize) / 2;
        context.drawImage(image, sourceX, sourceY, sourceSize, sourceSize, 0, 0, size, size);
        resolve(canvas.toDataURL('image/jpeg', 0.86).split(',')[1]);
      };
      image.onerror = reject;
      image.src = dataUrl;
    });
  }
}
