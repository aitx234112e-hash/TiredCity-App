import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FeedbackManagement } from './feedback-management';

describe('FeedbackManagement', () => {
  let component: FeedbackManagement;
  let fixture: ComponentFixture<FeedbackManagement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FeedbackManagement]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FeedbackManagement);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
