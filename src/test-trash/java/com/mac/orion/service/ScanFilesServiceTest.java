package com.mac.orion.service;

import com.mac.orion.BaseIntTest;
import com.mac.orion.application.service.ScanFilesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class ScanFilesServiceTest extends BaseIntTest {

    @Autowired
    private ScanFilesService scanFilesService;

    //@Test
    void givenFolderToScan_whenScanFilesAndProcessMD5_thenReturnAllFiles() throws InterruptedException {

/*
        // given
        final String folderToScan = "D:\\Download\\ORION-FILES\\share";
        final Integer folderItems = 6;

        // when
        final List<File> result = scanFilesService.scanFiles(folderToScan);

        // then
        result.forEach(file -> log.info("found file name: {} and size: {} and hash MD5: {}", file.name(), file.size(), file.hash()));
        log.info("Size of list of result: {}", result.size());
        assertEquals(folderItems, result.size());
*/

    }
}
