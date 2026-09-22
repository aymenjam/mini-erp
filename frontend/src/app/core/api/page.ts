/** Pagination shape of constitution.md section 4, hand-typed (no generated client, scope item 2). */
export interface Page<T> {
  readonly content: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}
