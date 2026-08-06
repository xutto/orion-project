package com.mac.orion.domain.model.settings;

import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@Deprecated
public record Scan(List<String> selected, List<String> partial) {
}
