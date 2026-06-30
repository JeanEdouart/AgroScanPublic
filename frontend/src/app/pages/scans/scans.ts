import { DatePipe } from '@angular/common';
import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, switchMap, takeWhile, timer } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { ConfirmationService } from '../../core/confirmation.service';
import { NotificationService } from '../../core/notification.service';
import { Plot, PlotService } from '../../core/plot.service';
import { PageResponse, ScanAnalysis, ScanAnalysisJob, ScanDetail, ScanInsights, ScanService, ScanSummary, ScanAnalysisStatus } from '../../core/scan.service';
import { ScanInsightsComponent } from './scan-insights/scan-insights';

type RelationMode = 'uploadPlot' | 'workflowPlot' | 'uploadFollowUp' | 'workflowFollowUp';

@Component({
  selector: 'app-scans',
  imports: [DatePipe, ReactiveFormsModule, ScanInsightsComponent],
  templateUrl: './scans.html',
  styleUrl: './scans.scss',
})
export class Scans {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly scanService = inject(ScanService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly plotService = inject(PlotService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly uploadDialog = viewChild.required<ElementRef<HTMLDialogElement>>('uploadDialog');
  private readonly uploadFileInput = viewChild<ElementRef<HTMLInputElement>>('uploadFileInput');
  private readonly detailDialog = viewChild.required<ElementRef<HTMLDialogElement>>('detailDialog');
  private readonly plotDialog = viewChild.required<ElementRef<HTMLDialogElement>>('plotDialog');
  private readonly relationDialog = viewChild.required<ElementRef<HTMLDialogElement>>('relationDialog');

  protected readonly results = signal<PageResponse<ScanSummary> | null>(null);
  protected readonly insights = signal<ScanInsights | null>(null);
  protected readonly plots = signal<Plot[]>([]);
  protected readonly relationPlotOptions = signal<Plot[]>([]);
  protected readonly relationScanOptions = signal<ScanSummary[]>([]);
  protected readonly relationMode = signal<RelationMode | null>(null);
  protected readonly relationSelectedId = signal<number | null>(null);
  protected readonly relationSelectedLabel = signal<string | null>(null);
  protected readonly uploadPlotLabel = signal<string | null>(null);
  protected readonly uploadFollowUpLabel = signal<string | null>(null);
  protected readonly workflowPlotLabel = signal<string | null>(null);
  protected readonly workflowFollowUpLabel = signal<string | null>(null);
  protected readonly selectedScan = signal<ScanDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly uploadLoading = signal(false);
  protected readonly detailLoading = signal(false);
  protected readonly deleteLoading = signal<number | null>(null);
  protected readonly workflowLoading = signal(false);
  protected readonly plotLoading = signal(false);
  protected readonly analysisLoading = signal<Set<number>>(new Set());
  protected readonly error = signal<string | null>(null);
  protected readonly uploadError = signal<string | null>(null);
  protected readonly previewUrl = signal<string | null>(null);
  protected readonly imageBase64 = signal<string | null>(null);
  protected readonly thumbnailBase64 = signal<string | null>(null);
  protected readonly imageMediaType = signal<string | null>(null);
  protected readonly ascending = signal(false);
  protected readonly page = signal(0);

  protected readonly searchForm = this.fb.group({
    name: [''],
    plotName: [''],
    favorite: [''],
    archiveState: ['active'],
    plant: [''],
    disease: [''],
    status: [''],
    healthy: [''],
    uploadedAt: [''],
  });

  protected readonly relationPickerForm = this.fb.group({ search: [''] });

  protected readonly uploadForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    plotId: [''],
    notes: ['', [Validators.maxLength(4000)]],
    followUpOfId: [''],
  });

  protected readonly workflowForm = this.fb.group({
    favorite: [false],
    archived: [false],
    plotId: [''],
    notes: ['', [Validators.maxLength(4000)]],
    followUpOfId: [''],
  });

  protected readonly plotForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    description: ['', [Validators.maxLength(1000)]],
  });

  constructor() {
    this.load();
    this.loadInsights();
    this.loadPlots();
    this.route.queryParamMap.subscribe((params) => {
      const scanId = Number(params.get('scanId'));
      if (Number.isFinite(scanId) && scanId > 0) {
        window.setTimeout(() => this.openDetail(scanId));
        void this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { scanId: null },
          queryParamsHandling: 'merge',
          replaceUrl: true,
        });
      }
    });
  }

  protected search(): void {
    this.page.set(0);
    this.load();
  }

  protected clearSearch(): void {
    this.searchForm.reset();
    this.searchForm.controls.archiveState.setValue('active');
    this.page.set(0);
    this.load();
  }

  protected filterByPlot(plotName: string): void {
    if (plotName === 'Sans parcelle') return;
    this.searchForm.controls.plotName.setValue(plotName);
    this.page.set(0);
    this.load();
  }

  protected filterByDisease(diseaseLabel: string): void {
    this.searchForm.controls.disease.setValue(diseaseLabel);
    this.page.set(0);
    this.load();
  }

  protected toggleSort(): void {
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

  protected openUpload(): void {
    this.uploadError.set(null);
    this.resetUpload();
    this.uploadDialog().nativeElement.showModal();
  }

  protected closeUpload(): void {
    this.uploadDialog().nativeElement.close();
    if (!this.uploadLoading()) this.resetUpload();
  }

  protected closeDetail(): void {
    this.detailDialog().nativeElement.close();
    this.selectedScan.set(null);
  }

  protected openPlots(): void {
    this.plotForm.reset();
    this.plotDialog().nativeElement.showModal();
  }

  protected closePlots(): void {
    this.plotDialog().nativeElement.close();
  }

  protected async selectFile(event: Event): Promise<void> {
    const file = (event.target as HTMLInputElement).files?.[0];
    this.uploadError.set(null);
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      this.uploadError.set('Sélectionnez une image JPEG, PNG ou WebP.');
      this.clearFileInput();
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.uploadError.set("L'image doit peser moins de 5 Mo.");
      this.clearFileInput();
      return;
    }
    const dataUrl = await this.readFile(file);
    const dimensions = await this.imageDimensions(dataUrl).catch(() => null);
    if (!dimensions) {
      this.uploadError.set("L'image sélectionnée est illisible.");
      this.clearFileInput();
      return;
    }
    if (Math.max(dimensions.width, dimensions.height) < 256) {
      this.uploadError.set("L'image est trop petite pour une analyse fiable.");
      this.clearFileInput();
      return;
    }
    this.previewUrl.set(dataUrl);
    this.imageBase64.set(dataUrl.split(',')[1]);
    this.imageMediaType.set(file.type);
    this.thumbnailBase64.set(await this.createThumbnail(dataUrl));
    if (!this.uploadForm.controls.name.value) {
      this.uploadForm.controls.name.setValue(file.name.replace(/\.[^.]+$/, ''));
    }
  }

  protected upload(): void {
    if (this.uploadForm.invalid || !this.imageBase64() || !this.thumbnailBase64() || !this.imageMediaType()) {
      this.uploadForm.markAllAsTouched();
      this.uploadError.set('Ajoutez une image, un nom et une description.');
      return;
    }
    this.uploadLoading.set(true);
    this.uploadError.set(null);
    const value = this.uploadForm.getRawValue();
    const progressId = this.notifications.newId('upload');
    const uploadName = value.name.trim() || "Image sans nom";
    this.showProgress(progressId, 'Importation du scan', `${uploadName} - Preparation de l'envoi`, 25, 'active');
    this.scanService.createWithProgress({
      name: value.name,
      description: value.description,
      imageBase64: this.imageBase64()!,
      thumbnailBase64: this.thumbnailBase64()!,
      imageMediaType: this.imageMediaType()!,
      notes: value.notes.trim() || undefined,
      plotId: this.parseOptionalId(value.plotId),
      followUpOfId: this.parseOptionalId(value.followUpOfId),
    }).pipe(finalize(() => this.uploadLoading.set(false))).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.Sent) {
          this.showProgress(progressId, 'Importation du scan', `${uploadName} - Envoi de l'image`, 40, 'active');
          return;
        }
        if (event.type === HttpEventType.UploadProgress && event.total) {
          const uploadProgress = Math.round((event.loaded / event.total) * 40);
          this.showProgress(progressId, 'Importation du scan', `${uploadName} - Envoi de l'image`, 40 + uploadProgress, 'active');
          return;
        }
        if (event.type !== HttpEventType.Response) return;
        const scanId = event.body?.id ?? null;
        this.showProgress(progressId, 'Importation du scan', `${uploadName} - Scan enregistre`, 100, 'success', scanId);
        this.closeUpload();
        this.resetUpload();
        this.page.set(0);
        this.load();
        this.loadInsights();
      },
      error: (error: HttpErrorResponse) => {
        const message = this.errorMessage(error);
        this.uploadError.set(message);
        this.showProgress(progressId, 'Importation du scan', `${uploadName} - ${message}`, 100, 'error');
      },
    });
  }

  protected async confirmUpload(): Promise<void> {
    if (this.uploadForm.invalid || !this.imageBase64() || !this.thumbnailBase64() || !this.imageMediaType()) {
      this.upload();
      return;
    }
    const confirmed = await this.confirmation.confirm({
      title: 'Ajouter ce scan ?',
      message: "L'image et sa description seront enregistrées dans votre espace.",
      confirmLabel: 'Ajouter le scan',
    });
    if (confirmed) this.upload();
  }

  protected openDetail(id: number): void {
    this.detailLoading.set(true);
    this.error.set(null);
    this.detailDialog().nativeElement.showModal();
    this.scanService.detail(id).pipe(finalize(() => this.detailLoading.set(false))).subscribe({
      next: (scan) => {
        this.selectedScan.set(scan);
        this.workflowForm.setValue({
          favorite: scan.favorite,
          archived: scan.archived,
          plotId: scan.plotId?.toString() ?? '',
          notes: scan.notes ?? '',
          followUpOfId: scan.followUpOfId?.toString() ?? '',
        });
        this.workflowPlotLabel.set(scan.plotName ?? null);
        this.workflowFollowUpLabel.set(scan.followUpName ?? null);
      },
      error: (error: HttpErrorResponse) => {
        this.closeDetail();
        this.error.set(this.errorMessage(error));
      },
    });
  }

  protected async deleteScan(scan: ScanSummary | ScanDetail): Promise<void> {
    const confirmed = await this.confirmation.confirm({
      title: 'Supprimer ce scan ?',
      message: `Le scan "${scan.name}" sera supprimé définitivement.`,
      confirmLabel: 'Supprimer',
      tone: 'danger',
    });
    if (!confirmed) return;

    this.deleteLoading.set(scan.id);
    this.error.set(null);
    this.scanService.delete(scan.id).pipe(finalize(() => this.deleteLoading.set(null))).subscribe({
      next: () => {
        if (this.selectedScan()?.id === scan.id) this.closeDetail();
        if (this.results()?.content.length === 1 && this.page() > 0) this.page.update((value) => value - 1);
        this.load();
        this.loadInsights();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.errorMessage(error)),
    });
  }

  protected analyzeScan(scan: ScanSummary | ScanDetail): void {
    const progressId = this.notifications.newId(`analysis-${scan.id}`);
    this.setAnalysisLoading(scan.id, true);
    this.updateScanStatus(scan.id, 'ANALYSIS_PENDING');
    this.error.set(null);
    this.showProgress(progressId, 'Analyse du scan', `${scan.name} - Mise en file de l'analyse`, 5, 'active', scan.id);
    this.scanService.analyze(scan.id, progressId, scan.name).subscribe({
      next: (job) => this.watchAnalysisJob(job, progressId, scan.name),
      error: (error: HttpErrorResponse) => {
        const message = this.errorMessage(error);
        this.setAnalysisLoading(scan.id, false);
        this.updateScanStatus(scan.id, 'ANALYSIS_FAILED');
        this.error.set(message);
        this.showProgress(progressId, 'Analyse du scan', `${scan.name} - ${message}`, 100, 'error', scan.id);
      },
    });
  }

  protected saveWorkflow(): void {
    const scan = this.selectedScan();
    if (!scan || this.workflowForm.invalid) {
      this.workflowForm.markAllAsTouched();
      return;
    }
    const value = this.workflowForm.getRawValue();
    this.workflowLoading.set(true);
    this.error.set(null);
    this.scanService.updateWorkflow(scan.id, {
      favorite: value.favorite,
      archived: value.archived,
      plotId: this.parseOptionalId(value.plotId),
      plotName: '',
      notes: value.notes.trim(),
      followUpOfId: this.parseOptionalId(value.followUpOfId),
    }).pipe(finalize(() => this.workflowLoading.set(false))).subscribe({
      next: (updated) => {
        this.selectedScan.set(updated);
        this.workflowPlotLabel.set(updated.plotName ?? null);
        this.workflowFollowUpLabel.set(updated.followUpName ?? null);
        this.results.update((results) => {
          if (!results) return results;
          return {
            ...results,
            content: results.content.map((item) => item.id === updated.id ? {
              ...item,
              favorite: updated.favorite,
              archived: updated.archived,
              plotId: updated.plotId,
              plotName: updated.plotName,
              followUpOfId: updated.followUpOfId,
              followUpName: updated.followUpName,
            } : item),
          };
        });
        this.loadInsights();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.errorMessage(error)),
    });
  }

  protected savePlot(): void {
    if (this.plotForm.invalid) {
      this.plotForm.markAllAsTouched();
      return;
    }
    const value = this.plotForm.getRawValue();
    this.plotLoading.set(true);
    this.plotService.create({
      name: value.name.trim(),
      description: value.description.trim(),
    }).pipe(finalize(() => this.plotLoading.set(false))).subscribe({
      next: () => {
        this.plotForm.reset();
        this.loadPlots();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.errorMessage(error)),
    });
  }

  protected deletePlot(plot: Plot): void {
    this.plotLoading.set(true);
    this.plotService.delete(plot.id).pipe(finalize(() => this.plotLoading.set(false))).subscribe({
      next: () => {
        this.loadPlots();
        this.load();
        this.loadInsights();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.errorMessage(error)),
    });
  }

  protected toggleWorkflowFavorite(): void {
    this.workflowForm.controls.favorite.setValue(!this.workflowForm.controls.favorite.value);
  }

  protected toggleWorkflowArchived(): void {
    this.workflowForm.controls.archived.setValue(!this.workflowForm.controls.archived.value);
  }

  protected toggleFavorite(scan: ScanSummary, event: MouseEvent): void {
    event.stopPropagation();
    this.quickUpdateWorkflow(scan, { favorite: !scan.favorite });
  }

  protected toggleArchived(scan: ScanSummary, event: MouseEvent): void {
    event.stopPropagation();
    this.quickUpdateWorkflow(scan, { archived: !scan.archived });
  }

  protected openRelationPicker(mode: RelationMode): void {
    this.relationMode.set(mode);
    this.relationPickerForm.reset();
    this.relationSelectedId.set(this.currentRelationId(mode));
    this.relationSelectedLabel.set(this.currentRelationLabel(mode));
    this.relationPlotOptions.set([]);
    this.relationScanOptions.set([]);
    this.relationDialog().nativeElement.showModal();
    this.searchRelationOptions();
  }

  protected closeRelationPicker(): void {
    this.relationDialog().nativeElement.close();
    this.relationMode.set(null);
  }

  protected searchRelationOptions(): void {
    const mode = this.relationMode();
    const search = this.relationPickerForm.controls.search.value.trim();
    if (!mode) return;
    if (this.isPlotMode(mode)) {
      this.plotService.list(search, 25).subscribe({
        next: (plots) => this.relationPlotOptions.set(plots),
        error: (error: HttpErrorResponse) => this.error.set(this.errorMessage(error)),
      });
      return;
    }
    this.scanService.search({
      name: search || undefined,
      page: 0,
      size: 10,
      ascending: false,
    }).subscribe({
      next: (results) => this.relationScanOptions.set(results.content.filter((scan) => scan.id !== this.selectedScan()?.id)),
      error: (error: HttpErrorResponse) => this.error.set(this.errorMessage(error)),
    });
  }

  protected selectRelation(id: number, label: string): void {
    this.relationSelectedId.set(id);
    this.relationSelectedLabel.set(label);
  }

  protected confirmRelationSelection(): void {
    const mode = this.relationMode();
    const id = this.relationSelectedId();
    const label = this.relationSelectedLabel();
    if (!mode || !id || !label) return;
    if (mode === 'uploadPlot') {
      this.uploadForm.controls.plotId.setValue(id.toString());
      this.uploadPlotLabel.set(label);
    } else if (mode === 'workflowPlot') {
      this.workflowForm.controls.plotId.setValue(id.toString());
      this.workflowPlotLabel.set(label);
    } else if (mode === 'uploadFollowUp') {
      this.uploadForm.controls.followUpOfId.setValue(id.toString());
      this.uploadFollowUpLabel.set(label);
    } else {
      this.workflowForm.controls.followUpOfId.setValue(id.toString());
      this.workflowFollowUpLabel.set(label);
    }
    this.closeRelationPicker();
  }

  protected clearUploadPlot(): void {
    this.uploadForm.controls.plotId.setValue('');
    this.uploadPlotLabel.set(null);
  }

  protected clearWorkflowPlot(): void {
    this.workflowForm.controls.plotId.setValue('');
    this.workflowPlotLabel.set(null);
  }

  protected clearUploadFollowUp(): void {
    this.uploadForm.controls.followUpOfId.setValue('');
    this.uploadFollowUpLabel.set(null);
  }

  protected clearWorkflowFollowUp(): void {
    this.workflowForm.controls.followUpOfId.setValue('');
    this.workflowFollowUpLabel.set(null);
  }

  protected relationTitle(): string {
    const mode = this.relationMode();
    return mode === 'uploadPlot' || mode === 'workflowPlot' ? 'Choisir une parcelle' : 'Choisir un scan precedent';
  }

  protected relationPlaceholder(): string {
    const mode = this.relationMode();
    return mode === 'uploadPlot' || mode === 'workflowPlot' ? 'Rechercher une parcelle' : 'Rechercher un scan';
  }

  protected isAnalysisLoading(scanId: number): boolean {
    return this.analysisLoading().has(scanId);
  }

  protected formatBytes(bytes: number): string {
    return bytes < 1024 * 1024 ? `${Math.round(bytes / 1024)} Ko` : `${(bytes / 1024 / 1024).toFixed(1)} Mo`;
  }

  protected statusLabel(status: ScanAnalysisStatus): string {
    return {
      UPLOADED: 'Importé',
      ANALYSIS_PENDING: 'En attente',
      ANALYSIS_RUNNING: 'Analyse en cours',
      ANALYSIS_DONE: 'Analysé',
      ANALYSIS_FAILED: 'Échec analyse',
    }[status];
  }

  protected healthyLabel(value: boolean | null): string {
    if (value === true) return 'Plante saine';
    if (value === false) return 'Maladie détectée';
    return 'État non déterminé';
  }

  protected scanRelationLabel(scan: ScanSummary): string {
    return `${scan.name} - ${new Date(scan.uploadedAt).toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })}`;
  }

  private load(): void {
    const dateRange = this.parseDate(this.searchForm.controls.uploadedAt.value);
    if (this.searchForm.controls.uploadedAt.value && !dateRange) {
      this.error.set('Utilisez le format JJ/MM/AAAA hh:mm pour rechercher par date.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.scanService.search({
      name: this.searchForm.controls.name.value.trim() || undefined,
      favorite: this.parseOptionalBoolean(this.searchForm.controls.favorite.value),
      archived: this.parseArchiveState(this.searchForm.controls.archiveState.value),
      plotName: this.searchForm.controls.plotName.value.trim() || undefined,
      plant: this.searchForm.controls.plant.value.trim() || undefined,
      disease: this.searchForm.controls.disease.value.trim() || undefined,
      status: this.parseStatus(this.searchForm.controls.status.value),
      healthy: this.parseHealthy(this.searchForm.controls.healthy.value),
      uploadedFrom: dateRange?.from,
      uploadedTo: dateRange?.to,
      page: this.page(),
      size: 10,
      ascending: this.ascending(),
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (results) => this.results.set(results),
      error: (error: HttpErrorResponse) => this.error.set(this.errorMessage(error)),
    });
  }

  private parseDate(value: string): { from: string; to: string } | null {
    if (!value.trim()) return { from: '', to: '' };
    const match = /^(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})$/.exec(value.trim());
    if (!match) return null;
    if (+match[4] > 23 || +match[5] > 59) return null;
    const date = new Date(+match[3], +match[2] - 1, +match[1], +match[4], +match[5]);
    if (date.getFullYear() !== +match[3] || date.getMonth() !== +match[2] - 1 || date.getDate() !== +match[1]) return null;
    return { from: date.toISOString(), to: new Date(date.getTime() + 60_000).toISOString() };
  }

  private readFile(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  private createThumbnail(dataUrl: string): Promise<string> {
    return new Promise((resolve, reject) => {
      const image = new Image();
      image.onload = () => {
        const canvas = document.createElement('canvas');
        const scale = Math.min(1, 360 / Math.max(image.width, image.height));
        canvas.width = Math.round(image.width * scale);
        canvas.height = Math.round(image.height * scale);
        canvas.getContext('2d')?.drawImage(image, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL('image/jpeg', 0.78).split(',')[1]);
      };
      image.onerror = reject;
      image.src = dataUrl;
    });
  }

  private resetUpload(): void {
    this.uploadForm.reset();
    this.uploadPlotLabel.set(null);
    this.uploadFollowUpLabel.set(null);
    this.previewUrl.set(null);
    this.imageBase64.set(null);
    this.thumbnailBase64.set(null);
    this.imageMediaType.set(null);
    this.clearFileInput();
  }

  private loadInsights(): void {
    this.scanService.insights().subscribe({
      next: (insights) => this.insights.set(insights),
      error: () => undefined,
    });
  }

  private loadPlots(search = ''): void {
    this.plotService.list(search, 25).subscribe({
      next: (plots) => this.plots.set(plots),
      error: (error: HttpErrorResponse) => this.error.set(this.errorMessage(error)),
    });
  }

  private imageDimensions(dataUrl: string): Promise<{ width: number; height: number }> {
    return new Promise((resolve, reject) => {
      const image = new Image();
      image.onload = () => resolve({ width: image.width, height: image.height });
      image.onerror = reject;
      image.src = dataUrl;
    });
  }

  private parseStatus(value: string): ScanAnalysisStatus | undefined {
    return value ? value as ScanAnalysisStatus : undefined;
  }

  private parseHealthy(value: string): boolean | undefined {
    if (value === 'true') return true;
    if (value === 'false') return false;
    return undefined;
  }

  private parseOptionalBoolean(value: string): boolean | undefined {
    if (value === 'true') return true;
    if (value === 'false') return false;
    return undefined;
  }

  private parseArchiveState(value: string): boolean | undefined {
    if (value === 'archived') return true;
    if (value === 'active') return false;
    return undefined;
  }

  private parseOptionalId(value: string): number | null {
    const trimmed = value.trim();
    if (!trimmed) return null;
    const id = Number(trimmed);
    return Number.isInteger(id) && id > 0 ? id : null;
  }

  private isPlotMode(mode: RelationMode): boolean {
    return mode === 'uploadPlot' || mode === 'workflowPlot';
  }

  private currentRelationId(mode: RelationMode): number | null {
    if (mode === 'uploadPlot') return this.parseOptionalId(this.uploadForm.controls.plotId.value);
    if (mode === 'workflowPlot') return this.parseOptionalId(this.workflowForm.controls.plotId.value);
    if (mode === 'uploadFollowUp') return this.parseOptionalId(this.uploadForm.controls.followUpOfId.value);
    return this.parseOptionalId(this.workflowForm.controls.followUpOfId.value);
  }

  private currentRelationLabel(mode: RelationMode): string | null {
    if (mode === 'uploadPlot') return this.uploadPlotLabel();
    if (mode === 'workflowPlot') return this.workflowPlotLabel();
    if (mode === 'uploadFollowUp') return this.uploadFollowUpLabel();
    return this.workflowFollowUpLabel();
  }

  private quickUpdateWorkflow(scan: ScanSummary, workflow: { favorite?: boolean; archived?: boolean }): void {
    this.scanService.updateWorkflow(scan.id, {
      ...workflow,
      plotId: scan.plotId,
      plotName: '',
      followUpOfId: scan.followUpOfId,
    }).subscribe({
      next: (updated) => {
        this.results.update((results) => {
          if (!results) return results;
          return {
            ...results,
            content: results.content.map((item) => item.id === updated.id ? {
              ...item,
              favorite: updated.favorite,
              archived: updated.archived,
              plotId: updated.plotId,
              plotName: updated.plotName,
              followUpOfId: updated.followUpOfId,
              followUpName: updated.followUpName,
            } : item),
          };
        });
      },
      error: (error: HttpErrorResponse) => this.error.set(this.errorMessage(error)),
    });
  }

  private clearFileInput(): void {
    const input = this.uploadFileInput()?.nativeElement;
    if (input) input.value = '';
  }

  private applyAnalysis(scanId: number, analysis: ScanAnalysis): void {
    this.results.update((results) => {
      if (!results) return results;
      return {
        ...results,
        content: results.content.map((scan) => scan.id === scanId ? { ...scan, analysis, analysisStatus: 'ANALYSIS_DONE' } : scan),
      };
    });
    this.selectedScan.update((scan) => scan && scan.id === scanId ? { ...scan, analysis, analysisStatus: 'ANALYSIS_DONE' } : scan);
    this.loadInsights();
  }

  private watchAnalysisJob(initialJob: ScanAnalysisJob, progressId: string, scanName: string): void {
    this.updateAnalysisProgress(initialJob, progressId, scanName);
    if (this.isAnalysisJobDone(initialJob)) {
      this.finishAnalysisJob(initialJob, progressId, scanName);
      return;
    }
    timer(700, 900).pipe(
      switchMap(() => this.scanService.analysisJob(initialJob.id)),
      takeWhile((job) => !this.isAnalysisJobDone(job), true),
    ).subscribe({
      next: (job) => {
        this.updateAnalysisProgress(job, progressId, scanName);
        if (this.isAnalysisJobDone(job)) this.finishAnalysisJob(job, progressId, scanName);
      },
      error: (error: HttpErrorResponse) => {
        this.recoverAnalysisAfterJobError(initialJob.scanId, progressId, scanName, error);
      },
    });
  }

  private updateAnalysisProgress(job: ScanAnalysisJob, progressId: string, scanName: string): void {
    if (job.status === 'RUNNING') this.updateScanStatus(job.scanId, 'ANALYSIS_RUNNING');
    this.showProgress(
            progressId,
            'Analyse du scan',
            `${scanName} - ${job.message}`,
            job.progress,
            job.status === 'FAILED' ? 'error' : 'active',
            job.scanId
    );
  }

  private finishAnalysisJob(job: ScanAnalysisJob, progressId: string, scanName: string): void {
    this.setAnalysisLoading(job.scanId, false);
    if (job.status === 'COMPLETED' && job.analysis) {
      this.applyAnalysis(job.scanId, job.analysis);
      this.showProgress(progressId, 'Analyse du scan', `${scanName} - Diagnostic disponible`, 100, 'success', job.scanId);
      return;
    }
    if (job.status === 'COMPLETED') {
      this.recoverAnalysisAfterJobError(job.scanId, progressId, scanName);
      return;
    }
    const message = job.error ?? "L'analyse a echoue.";
    this.updateScanStatus(job.scanId, 'ANALYSIS_FAILED');
    this.error.set(message);
    this.loadInsights();
    this.showProgress(progressId, 'Analyse du scan', `${scanName} - ${message}`, 100, 'error', job.scanId);
  }

  private isAnalysisJobDone(job: ScanAnalysisJob): boolean {
    return job.status === 'COMPLETED' || job.status === 'FAILED';
  }

  private recoverAnalysisAfterJobError(
    scanId: number,
    progressId: string,
    scanName: string,
    originalError: HttpErrorResponse | null = null,
  ): void {
    this.scanService.detail(scanId).subscribe({
      next: (scan) => {
        this.selectedScan.update((selected) => selected?.id === scanId ? scan : selected);
        if (scan.analysis) {
          this.applyAnalysis(scanId, scan.analysis);
          this.setAnalysisLoading(scanId, false);
          this.loadInsights();
          this.showProgress(progressId, 'Analyse du scan', `${scanName} - Diagnostic disponible`, 100, 'success', scanId);
          return;
        }
        this.failAnalysisProgress(scanId, progressId, scanName, originalError);
      },
      error: (error: HttpErrorResponse) => this.failAnalysisProgress(scanId, progressId, scanName, originalError ?? error),
    });
  }

  private failAnalysisProgress(
    scanId: number,
    progressId: string,
    scanName: string,
    error: HttpErrorResponse | null,
  ): void {
    const message = error ? this.errorMessage(error) : "L'analyse a echoue.";
    this.setAnalysisLoading(scanId, false);
    this.updateScanStatus(scanId, 'ANALYSIS_FAILED');
    this.error.set(message);
    this.showProgress(progressId, 'Analyse du scan', `${scanName} - ${message}`, 100, 'error', scanId);
  }

  private showProgress(
    id: string,
    label: string,
    message: string,
    progress: number,
    tone: 'active' | 'success' | 'error',
    scanId: number | null = null,
  ): void {
    this.notifications.upsert({
      id,
      label,
      message,
      progress: Math.max(0, Math.min(100, progress)),
      tone,
      scanId,
    });
  }

  private setAnalysisLoading(scanId: number, loading: boolean): void {
    this.analysisLoading.update((loadingIds) => {
      const next = new Set(loadingIds);
      if (loading) next.add(scanId);
      else next.delete(scanId);
      return next;
    });
  }

  private updateScanStatus(scanId: number, status: ScanAnalysisStatus): void {
    this.results.update((results) => {
      if (!results) return results;
      return {
        ...results,
        content: results.content.map((scan) => scan.id === scanId ? { ...scan, analysisStatus: status } : scan),
      };
    });
    this.selectedScan.update((scan) => scan && scan.id === scanId ? { ...scan, analysisStatus: status } : scan);
  }

  private errorMessage(error: HttpErrorResponse): string {
    return (error.error as ApiError | undefined)?.message ?? 'Une erreur est survenue. Veuillez réessayer.';
  }
}
