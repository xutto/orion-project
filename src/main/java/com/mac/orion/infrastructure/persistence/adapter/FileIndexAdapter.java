package com.mac.orion.infrastructure.persistence.adapter;

import com.mac.orion.application.out.FileIndexUseCase;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileIndexAdapter implements FileIndexUseCase {

  public static final String FIELD_HASH = "hash";
  public static final String FIELD_NAME = "name";

  private final Analyzer analyzer;
  private final IndexWriter indexWriter;
  private final SearcherManager searcherManager;

  @Override
  public void indexFile(File file) throws IOException {
    final Document doc = new Document();
    doc.add(new StringField(FIELD_HASH, file.getHash().getValue(), Store.YES));
    file.getNames().forEach(name -> doc.add(new TextField(FIELD_NAME, name, Store.NO)));

    // add or update the document
    indexWriter.updateDocument(new Term(FIELD_HASH, file.getHash().getValue()), doc);

    // commit changes to the index
    indexWriter.commit();

    // NRT: refresh Searcher to see recently indexed items
    searcherManager.maybeRefreshBlocking();
  }

  @Override
  public Set<Hash> findByNameWithLimit(String term, Integer limit) {
    IndexSearcher indexSearcher = null;
    final String normalizedTerm = analyzer.normalize(FIELD_NAME, term).utf8ToString();
    try {
      Set<Hash> result = new LinkedHashSet<>();
      BooleanQuery.Builder b = new BooleanQuery.Builder();
      for (String part : normalizedTerm.split("\\s+")) {
        if (!part.isEmpty()) {
          b.add(new PrefixQuery(new Term(FIELD_NAME, part)), BooleanClause.Occur.SHOULD);
        }
      }
      final Query query = b.build();
      indexSearcher = searcherManager.acquire();
      final TopDocs topDocs = indexSearcher.search(query, limit);
      for (ScoreDoc sd : topDocs.scoreDocs) {
        final Document document = indexSearcher.storedFields().document(sd.doc);
        result.add(Hash.builder().value(document.get(FIELD_HASH)).build());
      }
      return result;
    } catch (IOException e) {
      log.error("Error parsing query: {}", term, e);
      return new HashSet<>();
    } finally {
      try {
        searcherManager.release(indexSearcher);
      } catch (IOException ignore) {
      }
    }

  }

  @Deprecated
  private String escapeIfNeeded(String input) {
    try {
      return QueryParser.escape(input);
    } catch (Exception e) {
      return input;
    }
  }

}
