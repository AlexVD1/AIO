package com.storyvideo.api.export.dto;

import java.nio.file.Path;

public record ExportResult(
        Path videoFilePath,
        Path metadataFilePath,
        ExportMetadata metadata
) {}
