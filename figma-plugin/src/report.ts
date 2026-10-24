// Отчёт о построении: счётчики по видам объектов и предупреждения.

export type Counts = { created: number; updated: number; unchanged: number; deleted: number };

export type ReportData = { counts: Record<string, Counts>; warnings: string[] };

export class Report {
  private readonly counts: Record<string, Counts> = {};
  private readonly warnings: string[] = [];

  private bump(kind: string, field: keyof Counts) {
    this.counts[kind] ??= { created: 0, updated: 0, unchanged: 0, deleted: 0 };
    this.counts[kind][field] += 1;
  }

  created(kind: string) { this.bump(kind, "created"); }
  updated(kind: string) { this.bump(kind, "updated"); }
  unchanged(kind: string) { this.bump(kind, "unchanged"); }
  deleted(kind: string) { this.bump(kind, "deleted"); }
  warn(message: string) { this.warnings.push(message); }

  data(): ReportData {
    return { counts: this.counts, warnings: this.warnings };
  }
}
