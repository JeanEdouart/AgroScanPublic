import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Plot {
  id: number;
  name: string;
  description: string;
  createdAt: string;
}

export interface SavePlot {
  name: string;
  description: string;
}

@Injectable({ providedIn: 'root' })
export class PlotService {
  private readonly http = inject(HttpClient);

  list(search = '', limit = 25): Observable<Plot[]> {
    let params = new HttpParams().set('limit', limit);
    if (search.trim()) params = params.set('search', search.trim());
    return this.http.get<Plot[]>('/api/plots', { params });
  }

  create(plot: SavePlot): Observable<Plot> {
    return this.http.post<Plot>('/api/plots', plot);
  }

  update(id: number, plot: SavePlot): Observable<Plot> {
    return this.http.patch<Plot>(`/api/plots/${id}`, plot);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/plots/${id}`);
  }
}
