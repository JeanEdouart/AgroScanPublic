import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface SystemCheck {
  backend: 'UP';
  database: 'UP';
  message: string;
  checkedAt: string;
}

@Injectable({ providedIn: 'root' })
export class SystemCheckService {
  private readonly http = inject(HttpClient);

  getSystemCheck(): Observable<SystemCheck> {
    return this.http.get<SystemCheck>('/api/system-check');
  }
}
