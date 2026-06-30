import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, Observable, shareReplay, tap } from 'rxjs';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: 'USER' | 'ADMIN';
  profilePhotoDataUrl: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

export interface ProfileResponse {
  accessToken: string;
  user: User;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly accessTokenKey = 'agroscan.accessToken';
  private readonly refreshTokenKey = 'agroscan.refreshToken';
  private readonly userKey = 'agroscan.user';
  private refreshInFlight: Observable<AuthResponse> | null = null;

  readonly user = signal<User | null>(this.readUser());

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password })
      .pipe(tap((response) => this.storeSession(response)));
  }

  register(firstName: string, lastName: string, email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', { firstName, lastName, email, password })
      .pipe(tap((response) => this.storeSession(response)));
  }

  accessToken(): string | null {
    return localStorage.getItem(this.accessTokenKey);
  }

  loadCurrentUser(): Observable<User> {
    return this.http.get<User>('/api/users/me').pipe(tap((user) => this.storeUser(user)));
  }

  updateProfile(firstName: string, lastName: string, email: string): Observable<ProfileResponse> {
    return this.http
      .patch<ProfileResponse>('/api/users/me', { firstName, lastName, email })
      .pipe(tap((response) => {
        localStorage.setItem(this.accessTokenKey, response.accessToken);
        this.storeUser(response.user);
      }));
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>('/api/users/me/password', { currentPassword, newPassword });
  }

  updateProfilePhoto(imageBase64: string, imageMediaType: string): Observable<User> {
    return this.http
      .put<User>('/api/users/me/profile-photo', { imageBase64, imageMediaType })
      .pipe(tap((user) => this.storeUser(user)));
  }

  clearProfilePhoto(): Observable<User> {
    return this.http
      .delete<User>('/api/users/me/profile-photo')
      .pipe(tap((user) => this.storeUser(user)));
  }

  refreshAccessToken(): Observable<AuthResponse> {
    if (this.refreshInFlight) return this.refreshInFlight;
    const refreshToken = localStorage.getItem(this.refreshTokenKey);
    this.refreshInFlight = this.http
      .post<AuthResponse>('/api/auth/refresh', { refreshToken })
      .pipe(
        tap((response) => this.storeSession(response)),
        finalize(() => (this.refreshInFlight = null)),
        shareReplay(1),
      );
    return this.refreshInFlight;
  }

  logout(): void {
    const refreshToken = localStorage.getItem(this.refreshTokenKey);
    if (refreshToken) {
      this.http.post<void>('/api/auth/logout', { refreshToken }).subscribe();
    }
    this.clearSession();
    void this.router.navigateByUrl('/');
  }

  clearSession(): void {
    localStorage.removeItem(this.accessTokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.userKey);
    this.user.set(null);
  }

  private storeSession(response: AuthResponse): void {
    localStorage.setItem(this.accessTokenKey, response.accessToken);
    localStorage.setItem(this.refreshTokenKey, response.refreshToken);
    this.storeUser(response.user);
  }

  private storeUser(user: User): void {
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this.user.set(user);
  }

  private readUser(): User | null {
    const value = localStorage.getItem(this.userKey);
    if (!value) return null;
    try {
      return JSON.parse(value) as User;
    } catch {
      localStorage.removeItem(this.userKey);
      return null;
    }
  }
}
