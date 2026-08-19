import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';

import { authInterceptor } from './core/auth/auth.interceptor';
import { routes } from './app.routes';

// Les composants Material (bouton, form-field, toolbar) exigent que le moteur d'animations
// (réel ou noop) soit fourni ; provideAnimationsAsync le charge en différé plutôt que dans le
// bundle initial. prefers-reduced-motion reste respecté globalement (styles.scss).
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
