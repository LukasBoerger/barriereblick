import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

describe('authGuard', () => {
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockActivatedRoute: ActivatedRouteSnapshot;
  let mockRouterState: RouterStateSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: jasmine.createSpyObj('AuthService', [], { isAuthenticated: false })
        },
        {
          provide: Router,
          useValue: jasmine.createSpyObj('Router', ['createUrlTree'])
        }
      ]
    });

    mockAuthService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    mockRouter = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    // Setup mock route and router state
    mockActivatedRoute = {} as ActivatedRouteSnapshot;
    mockRouterState = {
      url: '/dashboard',
      root: {} as ActivatedRouteSnapshot
    } as RouterStateSnapshot;
  });

  it('should return true when user is authenticated', () => {
    Object.defineProperty(mockAuthService, 'isAuthenticated', { value: true, writable: true });

    TestBed.runInInjectionContext(() => {
      const result = authGuard(mockActivatedRoute, mockRouterState);
      expect(result).toBe(true);
    });
  });

  it('should return UrlTree redirecting to /login when not authenticated', () => {
    Object.defineProperty(mockAuthService, 'isAuthenticated', { value: false, writable: true });
    const mockUrlTree: UrlTree = {} as UrlTree;
    mockRouter.createUrlTree.and.returnValue(mockUrlTree);

    TestBed.runInInjectionContext(() => {
      const result = authGuard(mockActivatedRoute, mockRouterState);

      expect(result).toBe(mockUrlTree);
      expect(mockRouter.createUrlTree).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/dashboard' }
      });
    });
  });

  it('should include returnUrl query parameter with current URL', () => {
    Object.defineProperty(mockAuthService, 'isAuthenticated', { value: false, writable: true });
    const mockUrlTree: UrlTree = {} as UrlTree;
    mockRouter.createUrlTree.and.returnValue(mockUrlTree);

    mockRouterState.url = '/admin/settings';

    TestBed.runInInjectionContext(() => {
      authGuard(mockActivatedRoute, mockRouterState);

      expect(mockRouter.createUrlTree).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/admin/settings' }
      });
    });
  });

  it('should return UrlTree redirecting to /login when token is expired', () => {
    Object.defineProperty(mockAuthService, 'isAuthenticated', { value: false, writable: true });
    const mockUrlTree: UrlTree = {} as UrlTree;
    mockRouter.createUrlTree.and.returnValue(mockUrlTree);

    TestBed.runInInjectionContext(() => {
      const result = authGuard(mockActivatedRoute, mockRouterState);

      expect(result).toBe(mockUrlTree);
      expect(mockRouter.createUrlTree).toHaveBeenCalledWith(['/login'], jasmine.any(Object));
    });
  });

  it('should preserve root URL in returnUrl parameter', () => {
    Object.defineProperty(mockAuthService, 'isAuthenticated', { value: false, writable: true });
    const mockUrlTree: UrlTree = {} as UrlTree;
    mockRouter.createUrlTree.and.returnValue(mockUrlTree);

    mockRouterState.url = '/';

    TestBed.runInInjectionContext(() => {
      authGuard(mockActivatedRoute, mockRouterState);

      expect(mockRouter.createUrlTree).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/' }
      });
    });
  });
});
