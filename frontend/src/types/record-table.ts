export type RecordTableColumnType = 'text' | 'number' | 'datetime' | 'tag' | 'tags' | 'link';

export interface RecordTableTagValue {
  label: string;
  type?: 'success' | 'warning' | 'info' | 'danger' | 'primary';
}

export interface RecordTableLinkValue {
  label: string;
  href: string;
}

export interface RecordTableColumn {
  key: string;
  label: string;
  type?: RecordTableColumnType;
  sortable?: boolean;
  width?: number;
  minWidth?: number;
  fixed?: 'left' | 'right';
  align?: 'left' | 'center' | 'right';
  showOverflowTooltip?: boolean;
}
