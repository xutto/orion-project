package com.mac.orion.infrastructure.p2p.adapter;

import com.mac.orion.application.out.IdentifyExtractorUseCase;
import io.libp2p.core.Host;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentifyExtractorAdapter implements IdentifyExtractorUseCase {

  private final Host hostNode;
}
