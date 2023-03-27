import { WebPlugin } from '@capacitor/core';

import type { UcoachuPlugin } from './definitions';

export class UcoachuWeb extends WebPlugin implements UcoachuPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
