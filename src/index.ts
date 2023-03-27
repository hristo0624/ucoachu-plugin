import { registerPlugin } from '@capacitor/core';

import type { UcoachuPlugin } from './definitions';

const Ucoachu = registerPlugin<UcoachuPlugin>('Ucoachu', {
  web: () => import('./web').then(m => new m.UcoachuWeb()),
});

export * from './definitions';
export { Ucoachu };
