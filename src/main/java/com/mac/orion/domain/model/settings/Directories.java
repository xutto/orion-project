package com.mac.orion.domain.model.settings;

import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record Directories(List<String> scan,
                          String download,
                          String temp) {

}
