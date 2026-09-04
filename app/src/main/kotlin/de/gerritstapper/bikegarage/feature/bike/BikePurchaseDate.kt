package de.gerritstapper.bikegarage.feature.bike

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal fun String.toPurchaseDatePickerMillisOrNull(): Long? =
    runCatching {
        LocalDate.parse(this)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

internal fun Long.toPurchaseDateIso(): String =
    Instant.ofEpochMilli(this)
        .atOffset(ZoneOffset.UTC)
        .toLocalDate()
        .toString()
