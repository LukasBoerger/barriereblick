import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { TokenResponse } from './models/auth.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigateByUrl: jasmine.createSpy('navigateByUrl') } }
      ]
    }).compileComponents();

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);

    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    // Verify that no unmatched HTTP requests remain
    httpMock.verify();
    // Clear localStorage after each test
    localStorage.clear();
  });

  describe('constructor', () => {
    it('should load token from localStorage on init', () => {
      TestBed.resetTestingModule();
      localStorage.clear();
      const token = 'existing-token-from-storage';
      localStorage.setItem('auth_token', token);

      TestBed.configureTestingModule({
        providers: [
          AuthService,
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: Router, useValue: { navigateByUrl: jasmine.createSpy('navigateByUrl') } }
        ]
      });

      const freshService = TestBed.inject(AuthService);
      expect(freshService.token).toBe(token);
    });

    it('should initialize with null token if localStorage is empty', () => {
      TestBed.resetTestingModule();
      localStorage.clear();

      TestBed.configureTestingModule({
        providers: [
          AuthService,
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: Router, useValue: { navigateByUrl: jasmine.createSpy('navigateByUrl') } }
        ]
      });

      const freshService = TestBed.inject(AuthService);
      expect(freshService.token).toBeNull();
    });
  });

  describe('login', () => {
    it('should store token and expiresAt in localStorage and BehaviorSubject', (done) => {
      const mockResponse: TokenResponse = {
        token: 'test-token-12345',
        expiresAt: new Date(Date.now() + 3600000).toISOString()
      };

      service.login('test@example.com', 'password123').subscribe(() => {
        expect(localStorage.getItem('auth_token')).toBe('test-token-12345');
        expect(localStorage.getItem('token_expires_at')).toBe(mockResponse.expiresAt);
        expect(service.token).toBe('test-token-12345');
        done();
      });

      const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        email: 'test@example.com',
        password: 'password123'
      });
      req.flush(mockResponse);
    });

    it('should emit token via token$ observable after login', (done) => {
      const mockResponse: TokenResponse = {
        token: 'test-token-xyz',
        expiresAt: new Date(Date.now() + 3600000).toISOString()
      };

      let tokenReceived: string | null = null;
      service.token$.subscribe(token => {
        tokenReceived = token;
      });

      service.login('user@example.com', 'pass').subscribe(() => {
        expect(tokenReceived).toBe('test-token-xyz');
        done();
      });

      const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
      req.flush(mockResponse);
    });

    it('should propagate HTTP error', (done) => {
      service.login('wrong@example.com', 'wrongpass').subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error).toBeDefined();
          done();
        }
      );

      const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
      req.error(new ErrorEvent('Unauthorized'), { status: 401 });
    });
  });

  describe('logout', () => {
    it('should remove token and expiresAt from localStorage', () => {
      localStorage.setItem('auth_token', 'test-token');
      localStorage.setItem('token_expires_at', new Date().toISOString());

      service.logout();

      expect(localStorage.getItem('auth_token')).toBeNull();
      expect(localStorage.getItem('token_expires_at')).toBeNull();
    });

    it('should set token BehaviorSubject to null', () => {
      // First set a token
      localStorage.setItem('auth_token', 'test-token');
      service['tokenSubject'].next('test-token');

      service.logout();

      expect(service.token).toBeNull();
    });

    it('should navigate to /login', () => {
      service.logout();

      expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
    });
  });

  describe('isAuthenticated', () => {
    it('should return true when token exists and is not expired', () => {
      const futureDate = new Date(Date.now() + 3600000); // 1 hour from now
      localStorage.setItem('auth_token', 'valid-token');
      localStorage.setItem('token_expires_at', futureDate.toISOString());
      service['tokenSubject'].next('valid-token');

      expect(service.isAuthenticated).toBe(true);
    });

    it('should return false when token is expired', () => {
      const pastDate = new Date(Date.now() - 3600000); // 1 hour ago
      localStorage.setItem('auth_token', 'expired-token');
      localStorage.setItem('token_expires_at', pastDate.toISOString());
      service['tokenSubject'].next('expired-token');

      expect(service.isAuthenticated).toBe(false);
    });

    it('should return false when token is missing from BehaviorSubject', () => {
      localStorage.setItem('token_expires_at', new Date(Date.now() + 3600000).toISOString());
      service['tokenSubject'].next(null);

      expect(service.isAuthenticated).toBe(false);
    });

    it('should return false when expiresAt is missing from localStorage', () => {
      localStorage.setItem('auth_token', 'token-no-expiry');
      localStorage.removeItem('token_expires_at');
      service['tokenSubject'].next('token-no-expiry');

      expect(service.isAuthenticated).toBe(false);
    });

    it('should return false when both token and expiresAt are missing', () => {
      localStorage.clear();
      service['tokenSubject'].next(null);

      expect(service.isAuthenticated).toBe(false);
    });

    it('should return false when expiresAt is invalid date string', () => {
      localStorage.setItem('auth_token', 'token');
      localStorage.setItem('token_expires_at', 'invalid-date');
      service['tokenSubject'].next('token');

      // Invalid date string will parse to NaN, which is < Date.now()
      expect(service.isAuthenticated).toBe(false);
    });
  });

  describe('token property', () => {
    it('should return current token value from BehaviorSubject', () => {
      service['tokenSubject'].next('current-token');

      expect(service.token).toBe('current-token');
    });

    it('should return null when no token is set', () => {
      service['tokenSubject'].next(null);

      expect(service.token).toBeNull();
    });
  });
});
