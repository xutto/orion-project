package com.mac.orion.protoP2P;

import java.io.IOException;
import lombok.Getter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

@Getter
public class LuceneManualTestConfiguration {

  private final Directory directory = new ByteBuffersDirectory(); // ram usag
  private final Analyzer analyzer = new StandardAnalyzer();
  private final IndexWriter indexWriter;
  private final SearcherManager searcherManager;

  public LuceneManualTestConfiguration() throws IOException {
    final IndexWriterConfig config = new IndexWriterConfig(analyzer);
    this.indexWriter = new IndexWriter(directory, config);
    this.searcherManager = new SearcherManager(indexWriter, new SearcherFactory());
  }


}
