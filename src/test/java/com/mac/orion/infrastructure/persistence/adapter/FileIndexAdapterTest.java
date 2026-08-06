package com.mac.orion.infrastructure.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mac.orion.BaseUnitTest;
import com.mac.orion.application.out.FileIndexUseCase;
import com.mac.orion.application.service.GroupingUpdateFilesService;
import com.mac.orion.domain.dht.FileRoutingDataHashTable;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.infrastructure.configuration.IndexConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test that fills the FileRoutingDataHashTable and verifies Lucene index/search via
 * FileIndexAdapter.
 */
@Slf4j
public class FileIndexAdapterTest extends BaseUnitTest {

  private FileIndexUseCase fileIndex;

  @BeforeEach
  void setUp() throws IOException {
    IndexConfiguration config = new IndexConfiguration();

    Directory directory = config.luceneDirectory();
    Analyzer analyzer = config.luceneAnalyzer();
    IndexWriter indexWriter = config.fileIndexWriter(directory, analyzer);
    SearcherManager searcherManager = config.searcherManager(indexWriter);

    this.fileIndex = new FileIndexAdapter(analyzer, indexWriter, searcherManager);

    // Routing table that indexes newly added files using FileIndexUseCase
    FileRoutingDataHashTable routingTable = new FileRoutingDataHashTable(fileIndex, new GroupingUpdateFilesService());
    buildFiles().forEach(routingTable::addFile);
  }

  @Test
  void indexOneFile_and_findByName_returnsItsHash() {
    final String hashValue = "abc123def456";

    // given the file routing table has been filled with files in the setup step

    // then: searching by a token of the name should find the file's hash
    Set<Hash> hits = fileIndex.findByNameWithLimit("demo", 10);
    log.info("Hits: {}", hits);

    Set<Hash> expectedHits = fileIndex.findByNameWithLimit("file", 10);

    assertEquals(hits, expectedHits, "Search result should be the same as expected");
    assertNotNull(hits, "Search result should not be null");
    assertFalse(hits.isEmpty(), "Search result should not be empty");
    assertTrue(hits.stream().anyMatch(h -> hashValue.equals(h.getValue())),
        "Indexed file hash should be returned by search");
  }

  @Test
  void findByNameWithLimit_withDifferentCases_returnsSameResults() {

    String hashValue = "abc123";
    final List<Hash> comparesCaseA = new ArrayList<>();

    // given the file routing table has been filled with files in the setup step

    comparesCaseA.addAll(fileIndex.findByNameWithLimit("presentación", 1));
    comparesCaseA.addAll(fileIndex.findByNameWithLimit("preSentación", 1));
    comparesCaseA.addAll(fileIndex.findByNameWithLimit("mi", 1));
    comparesCaseA.addAll(fileIndex.findByNameWithLimit("Mi", 1));

    assertFalse(comparesCaseA.isEmpty(), "Search result should not be empty");
    assertEquals(4, comparesCaseA.size(), "Search result should be 4");
    comparesCaseA.forEach(c -> assertEquals(hashValue, c.getValue(),
        "Indexed file hash should be returned by search"));
  }

  @Test
  void findByNameWithLimit_twoTokensFromDifferentFiles_returnsUnion() {
    // Tokens from different files: "demo-file.txt" and "Mi presentación.pptx"
    Set<Hash> hits = fileIndex.findByNameWithLimit("demo presentación", 10);

    assertNotNull(hits, "Search result should not be null");
    assertFalse(hits.isEmpty(), "Search result should not be empty");

    // Expect at least the two hashes from those files
    Set<String> values = hits.stream().map(Hash::getValue).collect(java.util.stream.Collectors.toSet());
    assertTrue(values.contains("abc123def456"), "Should include hash of demo-file.txt");
    assertTrue(values.contains("abc123"), "Should include hash of Mi presentación.pptx");
  }

  private Set<File> buildFiles() {

    return new HashSet<>(Arrays.asList(
        File.builder().names(Set.of("demo-file.txt"))
            .path("C:/tmp/demo-file.txt").size(1024L)
            .hash(Hash.builder().value("abc123def456").build()).peers(new HashMap<>()).build(),
        File.builder().names(Set.of("Mi presentación.pptx"))
            .path("C:/Documents/Mi presentación.pptx").size(1024L)
            .hash(Hash.builder().value("abc123").build()).peers(new HashMap<>()).build(),
        File.builder().names(Set.of("documento_español.pdf"))
            .path("C:/Users/José/documento_español.pdf").size(2048L)
            .hash(Hash.builder().value("def456").build()).peers(new HashMap<>()).build(),
        File.builder().names(Set.of("report#2023.xlsx")).path("C:/Work/report#2023.xlsx").size(512L)
            .hash(Hash.builder().value("ghi789").build()).peers(new HashMap<>()).build(),
        File.builder().names(Set.of("índice general.docx"))
            .path("D:/Documentos/índice general.docx").size(4096L)
            .hash(Hash.builder().value("jkl012").build()).peers(new HashMap<>()).build(),
        File.builder().names(Set.of("proyecto_final.zip")).path("E:/Backup/proyecto_final.zip")
            .size(8192L).hash(Hash.builder().value("mno345").build()).peers(new HashMap<>())
            .build(),
        File.builder().names(Set.of("foto cumpleaños.jpg")).path("C:/Pictures/foto cumpleaños.jpg")
            .size(3072L).hash(Hash.builder().value("pqr678").build()).peers(new HashMap<>())
            .build(),
        File.builder().names(Set.of("datos$importantes.csv")).path("D:/Work/datos$importantes.csv")
            .size(1536L).hash(Hash.builder().value("stu901").build()).peers(new HashMap<>())
            .build(),
        File.builder().names(Set.of("año_2023.txt")).path("C:/Logs/año_2023.txt").size(256L)
            .hash(Hash.builder().value("vwx234").build()).peers(new HashMap<>()).build(),
        File.builder().names(Set.of("mi-video.mp4")).path("E:/Videos/mi-video.mp4").size(10240L)
            .hash(Hash.builder().value("yza567").build()).peers(new HashMap<>()).build(),
        File.builder().names(Set.of("código_fuente.java")).path("D:/Projects/código_fuente.java")
            .size(768L).hash(Hash.builder().value("bcd890").build()).peers(new HashMap<>()).build()
    ));
  }
}
