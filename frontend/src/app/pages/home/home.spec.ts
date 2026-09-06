import { TestBed } from '@angular/core/testing';
import { Home } from './home';

describe('Home', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [Home] });
  });

  it('creates the component', () => {
    const fixture = TestBed.createComponent(Home);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders the key figures and feature cards', () => {
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('salles de réunion');
    expect(compiled.textContent).toContain('Zéro double réservation');
  });
});
