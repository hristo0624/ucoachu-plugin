export interface UcoachuPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
