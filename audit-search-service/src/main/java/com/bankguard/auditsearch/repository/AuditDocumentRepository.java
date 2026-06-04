package com.bankguard.auditsearch.repository;

import com.bankguard.auditsearch.document.AuditDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AuditDocumentRepository extends ElasticsearchRepository<AuditDocument, String> {
}
