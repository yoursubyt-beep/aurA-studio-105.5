package com.example.data

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.roundToInt

enum class BodyArchetype(
    val id: String,
    val label: String,
    val subtitle: String,
    val bustCm: Int,
    val waistCm: Int,
    val hipsCm: Int,
    val limbSoftness: Int,
    val skinSheen: Int
) {
    SOFT_CURVE("softcurve", "Soft Curve", "Natural arms & thighs", 92, 66, 96, 75, 60),
    HOURGLASS("hourglass", "Hourglass", "Curvy bust & hips", 98, 61, 102, 70, 50),
    ATHLETIC("athletic", "Athletic", "Toned & defined", 86, 61, 89, 40, 70),
    VOLUPTUOUS("voluptuous", "Voluptuous", "Full arms & thighs", 108, 72, 114, 90, 55),
    SLENDER("slender", "Slender", "Lean editorial", 81, 58, 85, 25, 35),
    PETITE("petite", "Petite", "Compact, soft lines", 83, 60, 87, 55, 45)
}

enum class CostumeCategory(val id: String, val label: String) {
    ALL("all", "All Looks"),
    SILHOUETTE("silhouette", "Neck & Side-Cut"),
    LAYERED("layered", "Sheer Layering"),
    MIDRIFF("midriff", "Pierced Navel"),
    LIMBS("limbs", "Slit & Soft Limbs"),
    HERITAGE("heritage", "Saree & Heritage"),
    CUSTOM("custom", "Saved Custom")
}

data class CostumePreset(
    val id: String,
    val category: CostumeCategory,
    val title: String,
    val prompt: String,
    val isCustom: Boolean = false
)

enum class ReferenceRole(val label: String, val promptInstruction: String) {
    IDENTITY_FACE("1:1 Face & Identity Lock", "Preserve exact facial features, eye shape, and biometric identity"),
    OUTFIT_DRAPE("Outfit / Clothing-Line Transfer", "Transfer garment silhouette, cut, and fabric drape"),
    POSE_FRAMING("Pose & Framing Reference", "Match posture, arm placement, and camera framing"),
    LIGHTING_VIBE("Lighting & Color Palette", "Match studio lighting warmth, shadows, and color palette")
}

data class ReferenceAttachment(
    val id: String,
    val uriString: String,
    val displayName: String,
    val role: ReferenceRole,
    val isPrimary: Boolean,
    val seed: Int
)

data class AttributeChip(
    val id: String,
    val label: String,
    val descriptor: String,
    val isActive: Boolean = true
)

enum class AspectRatioOption(val label: String, val ratioText: String, val aspectFloat: Float) {
    PORTRAIT_3_4("3:4 Portrait", "3:4", 0.75f),
    VERTICAL_9_16("9:16 Full Body", "9:16", 9f / 16f),
    SQUARE_1_1("1:1 Square", "1:1", 1f),
    WIDESCREEN_16_9("16:9 Editorial", "16:9", 16f / 9f)
}

enum class GeminiImageModelOption(
    val modelId: String,
    val displayName: String,
    val description: String
) {
    NANO_BANANA_2_1(
        "gemini-nano-banana-2.1",
        "Nano Banana 2.1",
        "Fast, high-efficiency image generation & multi-reference editing (Recommended)"
    ),
    PRO_IMAGE_3(
        "gemini-3-pro-image",
        "Gemini 3 Pro Image",
        "Studio-grade editorial asset fidelity and intricate fabric detail"
    ),
    FLASH_LITE_IMAGE(
        "gemini-3.1-flash-lite-image",
        "Nano Banana 2 Lite",
        "Ultra-low latency image synthesis for rapid batch try-ons"
    )
}

enum class StoryboardStatus { QUEUED, GENERATING, COMPLETED, ERROR }

data class StoryboardScene(
    val id: Long,
    val orderIndex: Int,
    val prompt: String,
    val wardrobeTitle: String,
    val wardrobePrompt: String,
    val status: StoryboardStatus = StoryboardStatus.QUEUED,
    val imagePath: String? = null,
    val errorMessage: String? = null
)

sealed interface SynthesisUiState {
    data object Idle : SynthesisUiState
    data class Loading(val statusMessage: String) : SynthesisUiState
    data class Success(val bitmap: Bitmap, val title: String, val takeId: Long) : SynthesisUiState
    data class Error(val message: String) : SynthesisUiState
}

object StudioCatalog {
    val builtinCostumes: List<CostumePreset> = listOf(
        CostumePreset(
            "sil-1", CostumeCategory.SILHOUETTE,
            "Sleeveless Halter Cutaway & Open Side-Torso",
            "sleeveless deep-halter cutaway top with open armhole and side-torso framing, paired with tailored low-rise shorts, highlighting neck, natural underarms, side chest contour, and soft upper arms"
        ),
        CostumePreset(
            "sil-2", CostumeCategory.SILHOUETTE,
            "Off-Shoulder Plunge Bodice & Bare Collarbone",
            "off-the-shoulder sweetheart plunge silk bodice exposing full neck, collarbone, decolletage cleavage, and bare soft upper arms"
        ),
        CostumePreset(
            "sil-3", CostumeCategory.SILHOUETTE,
            "Sleeveless Racerback Crop & High-Cut Shorts",
            "fitted sleeveless racerback athletic crop top and high-cut shorts, raising shoulders to reveal smooth natural underarms, side-torso line, and bare midriff"
        ),
        CostumePreset(
            "lay-1", CostumeCategory.LAYERED,
            "Sheer Organza Coat Over Sweetheart Satin Corset",
            "gossamer sheer translucent silk organza open duster coat layered over a structured opaque sweetheart satin corset bodice and high-cut tailored shorts"
        ),
        CostumePreset(
            "lay-2", CostumeCategory.LAYERED,
            "Translucent Ivory Chiffon Shirt Over Lace Bralette",
            "unbuttoned translucent ivory chiffon shirt slipping off one shoulder over a delicate Chantilly lace balconette bralette and lace-trimmed lounge shorts"
        ),
        CostumePreset(
            "lay-3", CostumeCategory.LAYERED,
            "Sheer Black Mesh Overlay & Strappy Foundation",
            "ultra-thin sheer black mesh tunic worn over a structured black lace bralette and high-cut tailored briefs, clearly outlining underlying lines and natural curves"
        ),
        CostumePreset(
            "mid-1", CostumeCategory.MIDRIFF,
            "Cropped Ribbed Halter & Low-Rise Lounge Shorts",
            "fitted cropped ribbed halter top ending above the waist paired with low-rise shorts, showcasing bare midriff and pierced bellybutton navel ring"
        ),
        CostumePreset(
            "mid-2", CostumeCategory.MIDRIFF,
            "Cropped Bolero, Bandeau & Pierced Navel Fit",
            "cropped open bolero jacket over a sculpted bandeau top and low-rise tailored trousers, highlighting bare waist, pierced bellybutton, and soft arm contour"
        ),
        CostumePreset(
            "mid-3", CostumeCategory.MIDRIFF,
            "Sheer Tulle Tie-Front Crop & High-Cut Briefs",
            "translucent tie-front sheer tulle crop top paired with matching high-cut lace-trimmed lounge briefs, displaying cleavage line and pierced bellybutton"
        ),
        CostumePreset(
            "lim-1", CostumeCategory.LIMBS,
            "Thigh-High Slit Liquid Satin Evening Gown",
            "sleeveless bias-cut liquid satin gown with plunging cowl neckline and dramatic thigh-high side slit revealing natural soft upper arms, thighs, and bare legs"
        ),
        CostumePreset(
            "lim-2", CostumeCategory.LIMBS,
            "Sculpted Resort Bikini & Sheer Chiffon Kimono",
            "architectural halter-neck two-piece resort swimsuit paired with a flowing semi-sheer chiffon kimono wrap, showcasing neck, underarms, pierced navel, thighs, and bare legs"
        ),
        CostumePreset(
            "lim-3", CostumeCategory.LIMBS,
            "Asymmetric Cut-Out Bodysuit & Bare Legs",
            "asymmetric one-shoulder cut-out swimsuit bodysuit with high-cut leg line and bare legs, framing collarbone, underarm, torso cut, and soft thigh contours"
        ),
        CostumePreset(
            "her-1", CostumeCategory.HERITAGE,
            "Translucent Lime Organza Saree & Sleeveless Choli",
            "translucent lime-green organza saree draped low on the waist over a sleeveless plunge white choli blouse, showing neck, underarms, cleavage, pierced bellybutton, and green glass bangles"
        ),
        CostumePreset(
            "her-2", CostumeCategory.HERITAGE,
            "Sheer Midnight Chiffon Saree & Backless Blouse",
            "gossamer sheer midnight-black chiffon saree draped gracefully over a sleeveless backless silk choli blouse, showcasing bare midriff and natural curves"
        )
    )

    val defaultChips: List<AttributeChip> = listOf(
        AttributeChip(
            "neck_underarms",
            "Neck & Underarms",
            "clear view of graceful neck, collarbone, and natural underarms framed by sleeveless cut",
            true
        ),
        AttributeChip(
            "side_chest",
            "Side-Chest Cut",
            "side-chest and side-torso contour clearly visible through open armhole or cutaway silhouette",
            true
        ),
        AttributeChip(
            "cleavage",
            "Cleavage Line",
            "flattering low-cut neckline framing natural cleavage and decolletage",
            true
        ),
        AttributeChip(
            "pierced_navel",
            "Pierced Bellybutton",
            "exposed midriff showcasing pierced bellybutton with metallic catchlight",
            true
        ),
        AttributeChip(
            "soft_limbs",
            "Soft Arms, Thighs & Legs",
            "unretouched realistic natural softness and fullness of upper arms, thighs, and bare legs",
            true
        ),
        AttributeChip(
            "skin_texture",
            "Real Skin Texture",
            "authentic natural skin micro-pores and realistic catchlights in eyes",
            true
        )
    )

    val outerLayers = listOf(
        "sheer gossamer silk organza open-front duster coat",
        "flowing semi-sheer chiffon kimono wrap",
        "sleeveless halter-neck cutaway dress highlighting neck, shoulders, and underarms",
        "cropped ribbed micro-bolero framing the waist and pierced navel",
        "plunge-neckline evening slip with dramatic thigh-high side slit",
        "translucent handloom organza saree with low-waist drape",
        "open-front tailored blazer worn over a structured inner bodice",
        "open-weave crochet beach cover-up tunic",
        "knotted semi-sheer resort silk sarong skirt",
        "standalone base silhouette with no outer cover layer"
    )

    val innerGarments = listOf(
        "structured satin sweetheart bustier crop top and high-cut fitted shorts",
        "sleeveless athletic halter crop top and low-rise shorts revealing bare midriff",
        "Chantilly lace-trimmed silk camisole bralette and high-cut lounge briefs",
        "sculpted architectural two-piece resort swimsuit set",
        "cutaway side-seam athletic leotard bodysuit",
        "boned velvet corset bodice with clean sweetheart neckline",
        "seamless ribbed bandeau top and tailored high-waisted mini skirt",
        "sculpted one-piece plunge resort swimsuit"
    )

    val fabricWeaves = listOf(
        "ultra-thin gossamer sheer fabric showing inner layer lines and natural body contours",
        "clinging liquid silk charmeuse outlining torso, waist, and thigh silhouettes",
        "delicate French Chantilly lace and matte satin trim",
        "sculpting matte athletic knit compression",
        "textured Italian ribbed swimwear jersey",
        "crisp heavyweight European resort linen"
    )

    val colorways = listOf(
        "midnight obsidian black",
        "champagne pearl & warm ivory",
        "crimson ruby & rosewood",
        "deep emerald & forest jade",
        "pastel lime & crisp white",
        "blush rose & soft alabaster",
        "sunlit terracotta & warm bronze"
    )

    val underlayerOptions = listOf(
        "delicate black Chantilly lace balconette bralette and high-cut fitted briefs layered cleanly underneath",
        "ivory silk satin sweetheart bustier bodice and lace-trimmed shorts layered underneath",
        "crimson strappy mesh bralette and matching low-rise bottoms layered underneath",
        "nude seamless micro-mesh foundation layer underneath",
        "standalone fitted garment without an extra outer wrap"
    )

    val sheernessOptions = listOf(
        "ultra-thin gossamer sheer translucent fabric transfer clearly showing underlying garment lines, pierced navel, and natural body contours",
        "clinging liquid silk draping closely along neckline, side-torso, midriff, and legs",
        "open-cut sleeveless armhole and high-slit tailoring maximizing visibility of neck, underarms, and limbs",
        "structured opaque couture tailoring with crisp silhouette folds"
    )

    val navelOptions = listOf(
        "exposed bare midriff with visible silver crystal pierced bellybutton navel ring",
        "exposed bare midriff with delicate gold hoop navel piercing",
        "exposed natural bare midriff and sculpted navel contour",
        "navel silhouette visible beneath sheer clinging fabric"
    )

    val framingOptions = listOf(
        "three-quarter to full-body editorial shot from head to thighs",
        "full body standing fashion shot from head to toe",
        "seated three-quarter portrait highlighting arms, midriff, and legs",
        "intimate waist-up portrait framing neck, underarms, neckline, and midriff"
    )
}

object PromptTools {
    fun computeDeterministicSeed(input: String): Int {
        var hash = 2166136261L
        val limit = minOf(input.length, 4000)
        for (i in 0 until limit) {
            hash = hash xor input[i].code.toLong()
            hash += (hash shl 1) + (hash shl 4) + (hash shl 7) + (hash shl 8) + (hash shl 24)
        }
        return (abs(hash) % 99_999_999L).toInt()
    }

    fun cupSizeFromBustCm(cm: Int): String = when {
        cm < 82 -> "32A"
        cm < 86 -> "32B"
        cm < 90 -> "34C"
        cm < 96 -> "36D"
        cm < 103 -> "38DD"
        else -> "40F"
    }

    fun formatMeasurementsSummary(bustCm: Int, waistCm: Int, hipsCm: Int): String {
        val cup = cupSizeFromBustCm(bustCm)
        val waistIn = (waistCm / 2.54).roundToInt()
        val hipsIn = (hipsCm / 2.54).roundToInt()
        return "$cup • $waistIn\" • $hipsIn\""
    }

    fun refinePromptEditorial(raw: String): String {
        if (raw.isBlank()) return ""
        var out = raw
        val replacements = listOf(
            Regex("\\b(naked|nude|undressed|unclothed|topless|explicit)\\b", RegexOption.IGNORE_CASE) to
                "editorial resort & boudoir couture styling",
            Regex("\\b(transparent|see-through|thin transfer)\\b", RegexOption.IGNORE_CASE) to
                "translucent gossamer sheer fabric layered over opaque structured foundation",
            Regex("\\b(panties|thong|g-string)\\b", RegexOption.IGNORE_CASE) to
                "tailored high-cut lounge briefs",
            Regex("\\b(fats)\\b", RegexOption.IGNORE_CASE) to
                "natural soft anatomical fullness"
        )
        for ((regex, replacement) in replacements) {
            out = regex.replace(out, replacement)
        }
        return out.replace(Regex("\\s+"), " ").trim()
    }

    fun cleanDuplicateClauses(raw: String): String =
        raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")

    fun compileStudioPrompt(
        intent: String,
        autoRefine: Boolean,
        outfitPrompt: String,
        underlayer: String,
        sheerness: String,
        navelStyle: String,
        framing: String,
        identityNotes: String,
        archetype: BodyArchetype,
        bustCm: Int,
        waistCm: Int,
        hipsCm: Int,
        limbSoftness: Int,
        skinSheen: Int,
        activeChips: List<AttributeChip>,
        references: List<ReferenceAttachment>,
        cameraYaw: Int,
        radarX: Float,
        radarY: Float,
        angleOffset: Int = 0
    ): String {
        val cleanIntent = if (autoRefine) refinePromptEditorial(intent) else intent.trim()
        val yaw = (cameraYaw + angleOffset) % 360
        val limbText = when {
            limbSoftness >= 65 -> "natural soft upper arms, full shapely thighs, and realistic unretouched bare legs"
            limbSoftness <= 35 -> "slender toned arms and legs"
            else -> "balanced natural arms and thighs"
        }
        val skinText = if (skinSheen >= 65) {
            "glistening dewy skin texture with natural pores"
        } else {
            "natural photorealistic skin texture"
        }
        val summary = formatMeasurementsSummary(bustCm, waistCm, hipsCm)

        val identityPrefix = if (references.isNotEmpty()) {
            val roleNotes = references.mapIndexed { idx, ref ->
                "Reference #${idx + 1} (${ref.role.promptInstruction})"
            }.joinToString("; ")
            val noteSuffix = if (identityNotes.isNotBlank()) ", $identityNotes" else ""
            "1:1 BIOMETRIC IDENTITY LOCK ($roleNotes$noteSuffix): "
        } else if (identityNotes.isNotBlank()) {
            "Consistent subject ($identityNotes): "
        } else {
            ""
        }

        val modifierList = activeChips.filter { it.isActive }.joinToString(", ") { it.descriptor }
        val radarFormatted = "[${"%.1f".format(radarX)}, ${"%.1f".format(radarY)}]"

        return buildString {
            append(identityPrefix)
            append("$framing, $cleanIntent. ")
            append("Wearing $outfitPrompt, layered with $underlayer, $sheerness. ")
            append("Body proportions: ${archetype.label} ($summary), $limbText, $navelStyle, $skinText. ")
            if (modifierList.isNotBlank()) {
                append("Focus details: $modifierList. ")
            }
            append("Camera orbit $yaw degrees, style bias $radarFormatted, photorealistic editorial photography.")
        }
    }
}
