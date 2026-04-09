package com.data.collection.platform.service;

import com.data.collection.platform.entity.GitlabMirrorTableRegistry;

record DatabaseBrowserTableContext(DatabaseBrowserTableDefinition definition, GitlabMirrorTableRegistry registry) {
}
