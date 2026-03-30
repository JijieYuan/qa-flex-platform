package com.data.collection.platform.entity;

import java.util.List;

public record MirrorPurgeResult(
    MirrorPurgeScope scope,
    int droppedMirrorTables,
    List<String> droppedTableNames,
    int truncatedTables,
    List<String> truncatedTableNames,
    boolean syncTimestampsReset) {
}
