import type {
  DrilldownActiveCell,
  DrilldownColumnGroup,
  DrilldownDetailColumn,
  DrilldownMatrixRow,
} from '../../types/drilldown-table';

export interface DrilldownFilterOption {
  label: string;
  value: string;
}

export interface DrilldownFilterField {
  key: string;
  label: string;
  width?: number;
  options: DrilldownFilterOption[];
}

export interface DrilldownFilterState {
  [key: string]: string;
}

export interface DrilldownStatCard {
  label: string;
  value: string;
}

export interface DrilldownBoardViewModel {
  chip: string;
  title: string;
  subtitle: string;
  filterFields: DrilldownFilterField[];
  initialFilters: DrilldownFilterState;
  stats: DrilldownStatCard[];
  boardTitle: string;
  boardDescription: string;
  drawerDescription: string;
  columnGroups: DrilldownColumnGroup[];
  rows: DrilldownMatrixRow[];
  detailColumns: DrilldownDetailColumn[];
}

export abstract class AbstractDrilldownTableDefinition {
  abstract buildViewModel(): DrilldownBoardViewModel;

  abstract buildDetails(activeCell: DrilldownActiveCell): Record<string, unknown>[];

  resetFilters(): DrilldownFilterState {
    return { ...this.buildViewModel().initialFilters };
  }

  getDrawerTags(filters: DrilldownFilterState, activeCell: DrilldownActiveCell | null): string[] {
    const tags = Object.values(filters).filter(Boolean);
    if (activeCell?.row?.rowLabel) {
      tags.push(activeCell.row.rowLabel);
    }
    return tags;
  }
}

