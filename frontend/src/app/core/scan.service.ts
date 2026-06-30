import { HttpClient, HttpEvent, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ScanSummary {
  id: number;
  name: string;
  description: string;
  thumbnailDataUrl: string;
  imageMediaType: string;
  imageSizeBytes: number;
  uploadedAt: string;
  favorite: boolean;
  archived: boolean;
  plotId: number | null;
  plotName: string | null;
  followUpOfId: number | null;
  followUpName: string | null;
  analysisStatus: ScanAnalysisStatus;
  analysis: ScanAnalysis | null;
}

export interface ScanDetail {
  id: number;
  name: string;
  description: string;
  imageDataUrl: string;
  imageMediaType: string;
  imageSizeBytes: number;
  uploadedAt: string;
  favorite: boolean;
  archived: boolean;
  notes: string;
  plotId: number | null;
  plotName: string | null;
  followUpOfId: number | null;
  followUpName: string | null;
  analysisStatus: ScanAnalysisStatus;
  analysis: ScanAnalysis | null;
}

export type ScanAnalysisStatus = 'UPLOADED' | 'ANALYSIS_PENDING' | 'ANALYSIS_RUNNING' | 'ANALYSIS_DONE' | 'ANALYSIS_FAILED';

export interface ScanAnalysis {
  plant: string;
  plantLabel: string;
  disease: string;
  diseaseLabel: string;
  healthy: boolean | null;
  confidence: number | null;
  advice: string[];
  rawJson: string | null;
  analyzedAt: string;
}

export interface ScanAnalysisJob {
  id: string;
  scanId: number;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  progress: number;
  message: string;
  error: string | null;
  createdAt: string;
  updatedAt: string;
  analysis: ScanAnalysis | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ScanInsights {
  total: number;
  uploaded: number;
  pending: number;
  running: number;
  analyzed: number;
  failed: number;
  healthy: number;
  diseased: number;
  attention: number;
  plots: PlotHealthInsight[];
  diseases: DiseaseInsight[];
}

export interface PlotHealthInsight {
  plotId: number | null;
  plotName: string;
  total: number;
  healthy: number;
  diseased: number;
  latestScanAt: string;
}

export interface DiseaseInsight {
  disease: string;
  diseaseLabel: string;
  occurrences: number;
  latestDetectedAt: string;
}

export interface ScanSearch {
  name?: string;
  favorite?: boolean;
  archived?: boolean;
  plotName?: string;
  status?: ScanAnalysisStatus;
  healthy?: boolean;
  plant?: string;
  disease?: string;
  uploadedFrom?: string;
  uploadedTo?: string;
  page: number;
  size: number;
  ascending: boolean;
}

export interface CreateScan {
  name: string;
  description: string;
  imageBase64: string;
  thumbnailBase64: string;
  imageMediaType: string;
  notes?: string;
  plotId?: number | null;
  plotName?: string;
  followUpOfId?: number | null;
}

export interface UpdateScanWorkflow {
  favorite?: boolean;
  archived?: boolean;
  notes?: string;
  plotId?: number | null;
  plotName?: string;
  followUpOfId?: number | null;
}

export interface CreatedScan {
  id: number;
  name: string;
  description: string;
  imageMediaType: string;
  imageSizeBytes: number;
  uploadedAt: string;
}

@Injectable({ providedIn: 'root' })
export class ScanService {
  private readonly http = inject(HttpClient);

  search(search: ScanSearch): Observable<PageResponse<ScanSummary>> {
    let params = new HttpParams()
      .set('page', search.page)
      .set('size', search.size)
      .set('ascending', search.ascending);
    if (search.name) params = params.set('name', search.name);
    if (search.favorite !== undefined) params = params.set('favorite', search.favorite);
    if (search.archived !== undefined) params = params.set('archived', search.archived);
    if (search.plotName) params = params.set('plotName', search.plotName);
    if (search.status) params = params.set('status', search.status);
    if (search.healthy !== undefined) params = params.set('healthy', search.healthy);
    if (search.plant) params = params.set('plant', search.plant);
    if (search.disease) params = params.set('disease', search.disease);
    if (search.uploadedFrom) params = params.set('uploadedFrom', search.uploadedFrom);
    if (search.uploadedTo) params = params.set('uploadedTo', search.uploadedTo);
    return this.http.get<PageResponse<ScanSummary>>('/api/scans', { params });
  }

  insights(): Observable<ScanInsights> {
    return this.http.get<ScanInsights>('/api/scans/insights');
  }

  create(scan: CreateScan): Observable<CreatedScan> {
    return this.http.post<CreatedScan>('/api/scans', scan);
  }

  createWithProgress(scan: CreateScan): Observable<HttpEvent<CreatedScan>> {
    return this.http.post<CreatedScan>('/api/scans', scan, {
      observe: 'events',
      reportProgress: true,
    });
  }

  detail(id: number): Observable<ScanDetail> {
    return this.http.get<ScanDetail>(`/api/scans/${id}`);
  }

  updateWorkflow(id: number, workflow: UpdateScanWorkflow): Observable<ScanDetail> {
    return this.http.patch<ScanDetail>(`/api/scans/${id}/workflow`, workflow);
  }

  analyze(id: number, notificationId: string, scanName: string): Observable<ScanAnalysisJob> {
    return this.http.post<ScanAnalysisJob>(`/api/scans/${id}/analysis`, { notificationId, scanName });
  }

  analysisJob(jobId: string): Observable<ScanAnalysisJob> {
    return this.http.get<ScanAnalysisJob>(`/api/scans/analysis-jobs/${jobId}`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/scans/${id}`);
  }
}
