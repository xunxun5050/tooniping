package com.webtoonhub.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AdminWebtoonUpsertRequest(
    @NotBlank(message = "platformCode는 필수입니다.")
    String platformCode,

    String externalId,

    @NotBlank(message = "title은 필수입니다.")
    String title,

    String author,

    String description,

    @NotBlank(message = "originalUrl은 필수입니다.")
    String originalUrl,

    @NotBlank(message = "status는 필수입니다.")
    String status,

    @NotNull(message = "isAdult는 필수입니다.")
    Boolean isAdult,

    @NotNull(message = "isActive는 필수입니다.")
    Boolean isActive,

    @NotEmpty(message = "genreCodes는 최소 1개 이상이어야 합니다.")
    List<String> genreCodes,

    @NotEmpty(message = "weekdayCodes는 최소 1개 이상이어야 합니다.")
    List<String> weekdayCodes,

    @Valid
    ThumbnailRequest thumbnail
) {
    public record ThumbnailRequest(String sourceUrl) {
    }
}
