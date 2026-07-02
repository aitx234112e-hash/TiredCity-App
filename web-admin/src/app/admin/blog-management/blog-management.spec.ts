import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BlogManagement } from './blog-management';

describe('BlogManagement', () => {
  let component: BlogManagement;
  let fixture: ComponentFixture<BlogManagement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BlogManagement]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BlogManagement);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
