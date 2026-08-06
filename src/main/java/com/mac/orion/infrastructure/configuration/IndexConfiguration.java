package com.mac.orion.infrastructure.configuration;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilterFactory;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Slf4j
public class IndexConfiguration {

  @Bean(destroyMethod = "close")
  public Directory luceneDirectory() {

    //can use in filesystem: new FSDirectory(Paths.get(indexPath));

    log.info("Lucene using in-memory ByteBuffersDirectory");
    return new ByteBuffersDirectory();
  }

//  @Bean(destroyMethod = "close")
//  public Analyzer luceneAnalyzer() {
//    return new StandardAnalyzer();
//  }

  @Bean(destroyMethod = "close")
  public Analyzer luceneAnalyzer() throws IOException {

    log.debug("Lucene using custom analyzer with next configurations available: \n");
    log.debug("Tokenizers: {}\n", TokenizerFactory.availableTokenizers());
    log.debug("TokenFilters: {}\n", TokenFilterFactory.availableTokenFilters());
    log.debug("CharFilters: {}\n", CharFilterFactory.availableCharFilters());

    return CustomAnalyzer.builder()
        .withTokenizer("standard")
        .addTokenFilter("lowercase")
        .addTokenFilter("asciifolding") // quita diacríticos
        .build();
  }

  @Bean
  public IndexWriter fileIndexWriter(Directory directory, Analyzer analyzer) throws IOException {
    final IndexWriterConfig config = new IndexWriterConfig(analyzer);
    return new IndexWriter(directory, config);
  }

  @Bean(destroyMethod = "close")
  public SearcherManager searcherManager(IndexWriter indexWriter) throws IOException {
    // SearcherManager bound to the writer enables Near-Real-Time search without needing to close/reopen readers
    return new SearcherManager(indexWriter, new SearcherFactory());
  }

}
