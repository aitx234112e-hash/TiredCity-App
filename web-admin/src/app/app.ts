import { Component, HostListener, signal, OnInit } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserApiService } from './user-api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit {
  protected readonly title = signal('my-app');
  showBackToTop = false;
  private lastScrollY = 0;

  constructor(private router: Router, private userApi: UserApiService) {}

  ngOnInit() {
    this.userApi.loadUserFromLocal();
  }

  @HostListener('window:scroll')
  onWindowScroll(): void {
    const currentY = window.scrollY || document.documentElement.scrollTop || 0;

    if (currentY < 180) {
      this.showBackToTop = false;
      this.lastScrollY = currentY;
      return;
    }

    this.showBackToTop = currentY > this.lastScrollY;
    this.lastScrollY = currentY;
  }

  scrollToTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
