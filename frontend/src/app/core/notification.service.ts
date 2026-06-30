import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';

export interface AppNotification {
  id: string;
  label: string;
  message: string;
  progress: number;
  tone: 'active' | 'success' | 'error';
  scanId: number | null;
  createdAt: string;
  updatedAt: string;
}

interface NotificationEvent {
  type: 'UPSERT' | 'DELETE' | 'REFRESH';
  payload: AppNotification | AppNotification[] | { id: string };
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly items = signal<AppNotification[]>([]);
  private socket: WebSocket | null = null;
  private reconnectHandle: number | null = null;
  private intentionallyDisconnected = false;

  readonly notifications = this.items.asReadonly();
  readonly count = computed(() => this.items().length);
  readonly hasActive = computed(() => this.items().some((notification) => notification.tone === 'active'));

  connect(): void {
    const token = this.auth.accessToken();
    if (!token) return;

    this.intentionallyDisconnected = false;
    this.load();

    if (this.socket && this.socket.readyState !== WebSocket.CLOSED) return;

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    this.socket = new WebSocket(`${protocol}://${window.location.host}/ws/notifications?token=${encodeURIComponent(token)}`);
    this.socket.onmessage = (event) => {
      try {
        this.applyEvent(JSON.parse(event.data) as NotificationEvent);
      } catch {
        this.load();
      }
    };
    this.socket.onerror = () => {
      this.socket?.close();
    };
    this.socket.onclose = () => {
      this.socket = null;
      this.scheduleReconnect();
    };
  }

  disconnect(): void {
    this.intentionallyDisconnected = true;
    this.cancelReconnect();
    this.socket?.close();
    this.socket = null;
    this.items.set([]);
  }

  upsert(notification: Omit<AppNotification, 'createdAt' | 'updatedAt'>): void {
    const now = new Date().toISOString();
    this.applyUpsert({ ...notification, createdAt: now, updatedAt: now });
    this.http.put<AppNotification>(`/api/notifications/${encodeURIComponent(notification.id)}`, notification)
      .subscribe((saved) => this.applyUpsert(saved));
  }

  dismiss(id: string): void {
    this.http.delete<void>(`/api/notifications/${encodeURIComponent(id)}`).subscribe(() => {
      this.items.update((items) => items.filter((item) => item.id !== id));
    });
  }

  clearFinished(): void {
    this.http.delete<void>('/api/notifications/finished').subscribe(() => {
      this.items.update((items) => items.filter((item) => item.tone === 'active'));
    });
  }

  newId(prefix: string): string {
    return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }

  private applyEvent(event: NotificationEvent): void {
    if (event.type === 'UPSERT' && !Array.isArray(event.payload) && 'label' in event.payload) {
      this.applyUpsert(event.payload);
      return;
    }
    if (event.type === 'DELETE' && !Array.isArray(event.payload) && 'id' in event.payload) {
      const payload = event.payload as { id: string };
      this.items.update((items) => items.filter((item) => item.id !== payload.id));
      return;
    }
    if (event.type === 'REFRESH' && Array.isArray(event.payload)) {
      this.items.set(event.payload);
    }
  }

  private applyUpsert(notification: AppNotification): void {
    this.items.update((items) => {
      const index = items.findIndex((item) => item.id === notification.id);
      if (index === -1) return [notification, ...items];
      const current = items[index];
      if (current.tone === 'success' && notification.tone !== 'success') return items;
      if (current.tone === 'error' && notification.tone === 'active') return items;
      if (
        current.updatedAt
        && notification.updatedAt
        && Date.parse(notification.updatedAt) < Date.parse(current.updatedAt)
      ) {
        return items;
      }
      return items.map((item) => item.id === notification.id ? notification : item);
    });
  }

  private load(): void {
    this.http.get<AppNotification[]>('/api/notifications').subscribe({
      next: (notifications) => this.items.set(notifications),
      error: () => undefined,
    });
  }

  private scheduleReconnect(): void {
    if (this.intentionallyDisconnected || this.reconnectHandle !== null) return;
    this.reconnectHandle = window.setTimeout(() => {
      this.reconnectHandle = null;
      this.connect();
    }, 3000);
  }

  private cancelReconnect(): void {
    if (this.reconnectHandle === null) return;
    window.clearTimeout(this.reconnectHandle);
    this.reconnectHandle = null;
  }
}
