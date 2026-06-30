import { afterNextRender, Component, ElementRef, HostBinding, HostListener, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  protected readonly auth = inject(AuthService);
  private readonly element = inject(ElementRef<HTMLElement>);

  @HostBinding('style.--scroll') protected scroll = '0';
  @HostBinding('style.--hero-scroll') protected heroScroll = '0';
  @HostBinding('style.--flow-scroll') protected flowScroll = '0';
  @HostBinding('style.--terrain-scroll') protected terrainScroll = '0';

  private animationFrame = 0;

  constructor() {
    afterNextRender(() => this.queueScrollUpdate());
  }

  @HostListener('window:scroll')
  @HostListener('window:resize')
  protected queueScrollUpdate(): void {
    if (this.animationFrame) return;
    this.animationFrame = window.requestAnimationFrame(() => {
      const page = document.documentElement;
      const max = Math.max(1, page.scrollHeight - window.innerHeight);
      this.scroll = (window.scrollY / max).toFixed(4);
      this.heroScroll = this.sectionProgress('.hero');
      this.flowScroll = this.sectionProgress('.flow');
      this.terrainScroll = this.sectionProgress('.terrain');
      this.animationFrame = 0;
    });
  }

  private sectionProgress(selector: string): string {
    const section = this.element.nativeElement.querySelector(selector) as HTMLElement | null;
    if (!section) return '0';

    const rect = section.getBoundingClientRect();
    const range = window.innerHeight + rect.height;
    const progress = (window.innerHeight - rect.top) / range;

    return Math.min(1, Math.max(0, progress)).toFixed(4);
  }
}
