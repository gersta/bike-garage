package de.gerritstapper.bikegarage.domain.model

import java.util.Locale

data class PartTechnicalField(
    val key: String,
    val label: String,
)

data class PartTypeDefinition(
    val name: String,
    val category: PartCategory,
    val technicalFields: List<PartTechnicalField>,
    val aliases: Set<String> = emptySet(),
)

object PartTechnicalFields {
    private val chainring = listOf(
        PartTechnicalField("gear_ratio", "Übersetzung"),
    )
    private val chain = listOf(
        PartTechnicalField("link_count", "Anzahl Glieder"),
        PartTechnicalField("gear_count", "Anzahl Gänge"),
    )
    private val cassette = listOf(
        PartTechnicalField("sprocket_count", "Anzahl Ritzel"),
        PartTechnicalField("gear_ratio", "Übersetzung"),
        PartTechnicalField("freehub", "Freilauf"),
    )
    private val rim = listOf(
        PartTechnicalField("spoke_count", "Anzahl Speichen"),
    )
    private val handlebar = listOf(
        PartTechnicalField("onepiece", "Onepiece"),
        PartTechnicalField("diameter", "Durchmesser"),
    )

    val types = listOf(
        PartTypeDefinition(
            name = "Kettenblatt",
            category = PartCategory.DRIVETRAIN,
            technicalFields = chainring,
            aliases = setOf("Blatt"),
        ),
        PartTypeDefinition("Kette", PartCategory.DRIVETRAIN, chain),
        PartTypeDefinition("Kassette", PartCategory.DRIVETRAIN, cassette),
        PartTypeDefinition("Bremsscheibe", PartCategory.BRAKES, emptyList()),
        PartTypeDefinition("Bremssattel", PartCategory.BRAKES, emptyList()),
        PartTypeDefinition("Bremshebel", PartCategory.BRAKES, emptyList()),
        PartTypeDefinition("Bremsbelag", PartCategory.BRAKES, emptyList()),
        PartTypeDefinition("Felge", PartCategory.WHEELS, rim),
        PartTypeDefinition("Nabe", PartCategory.WHEELS, emptyList()),
        PartTypeDefinition("Speiche", PartCategory.WHEELS, emptyList()),
        PartTypeDefinition("Reifen", PartCategory.WHEELS, emptyList()),
        PartTypeDefinition("Lenker", PartCategory.COCKPIT, handlebar),
        PartTypeDefinition("Vorbau", PartCategory.COCKPIT, emptyList()),
        PartTypeDefinition("Griffe", PartCategory.COCKPIT, emptyList()),
        PartTypeDefinition("Lenkerband", PartCategory.COCKPIT, emptyList()),
        PartTypeDefinition("Sattel", PartCategory.SADDLE, emptyList()),
        PartTypeDefinition("Sattelstütze", PartCategory.SADDLE, emptyList()),
        PartTypeDefinition("Gabel", PartCategory.SUSPENSION, emptyList()),
        PartTypeDefinition("Dämpfer", PartCategory.SUSPENSION, emptyList()),
        PartTypeDefinition("Pedale", PartCategory.ACCESSORIES, emptyList()),
        PartTypeDefinition("Schutzblech", PartCategory.ACCESSORIES, emptyList()),
        PartTypeDefinition("Gepäckträger", PartCategory.ACCESSORIES, emptyList()),
        PartTypeDefinition("Licht", PartCategory.ACCESSORIES, emptyList()),
    )

    fun forCategory(category: PartCategory?): List<PartTypeDefinition> =
        types.filter { it.category == category }

    fun forType(type: String?): List<PartTechnicalField> =
        definitionFor(type)?.technicalFields.orEmpty()

    fun isTypeForCategory(type: String?, category: PartCategory): Boolean =
        definitionFor(type)?.category == category

    fun canonicalType(type: String?): String? = definitionFor(type)?.name

    private fun definitionFor(type: String?): PartTypeDefinition? {
        val normalizedType = type?.trim()?.lowercase(Locale.ROOT) ?: return null
        return types.firstOrNull { definition ->
            definition.name.lowercase(Locale.ROOT) == normalizedType ||
                definition.aliases.any { it.lowercase(Locale.ROOT) == normalizedType }
        }
    }
}
