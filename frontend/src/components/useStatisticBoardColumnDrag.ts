import { reactive, type Ref } from 'vue';
import type { StatisticBoardViewPrefs } from './statistic-board-view-prefs';

export function useStatisticBoardColumnDrag(
  boardViewPrefs: Ref<StatisticBoardViewPrefs>,
  persistViewPrefs: () => void,
) {
  const dragState = reactive<{
    type: 'group' | 'column' | '';
    sourceGroupKey: string;
    sourceColumnKey: string;
  }>({
    type: '',
    sourceGroupKey: '',
    sourceColumnKey: '',
  });

  function onGroupDragStart(groupKey: string) {
    dragState.type = 'group';
    dragState.sourceGroupKey = groupKey;
    dragState.sourceColumnKey = '';
  }

  function isGroupDragging(groupKey: string) {
    return dragState.type === 'group' && dragState.sourceGroupKey === groupKey;
  }

  function onGroupDrop(targetGroupKey: string) {
    if (dragState.type !== 'group' || dragState.sourceGroupKey === targetGroupKey) {
      clearDragState();
      return;
    }

    const nextOrder = [...boardViewPrefs.value.groupOrder];
    const sourceIndex = nextOrder.indexOf(dragState.sourceGroupKey);
    const targetIndex = nextOrder.indexOf(targetGroupKey);
    if (sourceIndex < 0 || targetIndex < 0) {
      clearDragState();
      return;
    }
    const [moved] = nextOrder.splice(sourceIndex, 1);
    nextOrder.splice(targetIndex, 0, moved);
    boardViewPrefs.value = {
      ...boardViewPrefs.value,
      groupOrder: nextOrder,
    };
    persistViewPrefs();
    clearDragState();
  }

  function onColumnDragStart(groupKey: string, columnKey: string) {
    dragState.type = 'column';
    dragState.sourceGroupKey = groupKey;
    dragState.sourceColumnKey = columnKey;
  }

  function isColumnDragging(groupKey: string, columnKey: string) {
    return (
      dragState.type === 'column' &&
      dragState.sourceGroupKey === groupKey &&
      dragState.sourceColumnKey === columnKey
    );
  }

  function onColumnDrop(targetGroupKey: string, targetColumnKey: string) {
    if (
      dragState.type !== 'column' ||
      dragState.sourceGroupKey !== targetGroupKey ||
      dragState.sourceColumnKey === targetColumnKey
    ) {
      clearDragState();
      return;
    }

    const nextColumns = [...(boardViewPrefs.value.columnOrderByGroup[targetGroupKey] ?? [])];
    const sourceIndex = nextColumns.indexOf(dragState.sourceColumnKey);
    const targetIndex = nextColumns.indexOf(targetColumnKey);
    if (sourceIndex < 0 || targetIndex < 0) {
      clearDragState();
      return;
    }
    const [moved] = nextColumns.splice(sourceIndex, 1);
    nextColumns.splice(targetIndex, 0, moved);
    boardViewPrefs.value = {
      ...boardViewPrefs.value,
      columnOrderByGroup: {
        ...boardViewPrefs.value.columnOrderByGroup,
        [targetGroupKey]: nextColumns,
      },
    };
    persistViewPrefs();
    clearDragState();
  }

  function clearDragState() {
    dragState.type = '';
    dragState.sourceGroupKey = '';
    dragState.sourceColumnKey = '';
  }

  return {
    onGroupDragStart,
    isGroupDragging,
    onGroupDrop,
    onColumnDragStart,
    isColumnDragging,
    onColumnDrop,
    clearDragState,
  };
}
