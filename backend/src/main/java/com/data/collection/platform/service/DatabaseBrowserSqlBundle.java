package com.data.collection.platform.service;

import java.util.List;

record DatabaseBrowserSqlBundle(String countSql, String rowsSql, List<Object> arguments) {
}
