package com.mac.orion.integration;

import com.mac.orion.BaseIntTest;
import com.mac.orion.integration.protocol.FileInfoAvailabilityProtocol;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IntegrationHostNodesTest extends BaseIntTest {

    private final FileInfoAvailabilityProtocol fileInfoAvailabiltyProtocol;
}
