import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from './scan.service';

export type UserRole = 'USER' | 'ADMIN';

export interface AdminUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserSearch {
  search: string;
  page: number;
  size: number;
  sortBy: 'firstName' | 'lastName' | 'email' | 'role' | 'createdAt';
  ascending: boolean;
}

export interface UpdateAdminUser {
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  enabled: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly http = inject(HttpClient);

  search(search: AdminUserSearch): Observable<PageResponse<AdminUser>> {
    const params = new HttpParams()
      .set('search', search.search)
      .set('page', search.page)
      .set('size', search.size)
      .set('sortBy', search.sortBy)
      .set('ascending', search.ascending);
    return this.http.get<PageResponse<AdminUser>>('/api/admin/users', { params });
  }

  update(id: number, user: UpdateAdminUser): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`/api/admin/users/${id}`, user);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/users/${id}`);
  }
}
