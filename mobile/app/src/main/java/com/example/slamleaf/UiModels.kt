package com.example.slamleaf

import com.example.slamleaf.data.remote.DetectionDto

enum class PredictionStatus {
    PENDING,
    SUCCESS,
    ERROR
}

enum class DiseaseType(
    val classId: Int,
    val displayName: String
) {
    APPLE_SCAB(0, "Parch jabłoni"),
    APPLE_LEAF(1, "Liść jabłoni (zdrowy)"),
    APPLE_RUST(2, "Rdza jabłoni"),
    BELL_PEPPER_LEAF(3, "Liść papryki (zdrowy)"),
    BELL_PEPPER_LEAF_SPOT(4, "Plamistość liści papryki"),
    BLUEBERRY_LEAF(5, "Liść borówki (zdrowy)"),
    CHERRY_LEAF(6, "Liść czereśni (zdrowy)"),
    CORN_GRAY_LEAF_SPOT(7, "Szara plamistość liści kukurydzy"),
    CORN_LEAF_BLIGHT(8, "Zaraza liści kukurydzy"),
    CORN_RUST(9, "Rdza kukurydzy"),
    PEACH_LEAF(10, "Liść brzoskwini (zdrowy)"),
    POTATO_LEAF(11, "Liść ziemniaka (zdrowy)"),
    POTATO_EARLY_BLIGHT(12, "Alternarioza ziemniaka"),
    POTATO_LATE_BLIGHT(13, "Zaraza ziemniaka"),
    RASPBERRY_LEAF(14, "Liść maliny (zdrowy)"),
    SOYBEAN_LEAF(15, "Liść soi (zdrowy)"),
    SQUASH_POWDERY_MILDEW(17, "Mączniak prawdziwy dyniowatych"),
    STRAWBERRY_LEAF(18, "Liść truskawki (zdrowy)"),
    TOMATO_EARLY_BLIGHT(19, "Alternarioza pomidora"),
    TOMATO_SEPTORIA(20, "Septorioza liści pomidora"),
    TOMATO_LEAF(21, "Liść pomidora (zdrowy)"),
    TOMATO_BACTERIAL_SPOT(22, "Bakteryjna plamistość pomidora"),
    TOMATO_LATE_BLIGHT(23, "Zaraza ziemniaka (na pomidorze)"),
    TOMATO_MOSAIC_VIRUS(24, "Wirus mozaiki pomidora"),
    TOMATO_YELLOW_VIRUS(25, "Wirus żółtej kędzierzawości liści pomidora"),
    TOMATO_MOLD_LEAF(26, "Pleśń liści pomidora"),
    TOMATO_SPIDER_MITES(27, "Przędziorek chmielowiec"),

    GRAPE_LEAF(28, "Liść winorośli (zdrowy)"),
    GRAPE_BLACK_ROT(29, "Czarna zgnilizna winorośli"),

    UNKNOWN(-1, "Nieznana choroba");

    companion object {
        fun fromClassId(classId: Int): DiseaseType =
            values().firstOrNull { it.classId == classId } ?: UNKNOWN
    }
}


data class UiPhotoItem(
    val localId: Long,
    val localPath: String,
    val processedUrl: String?,
    val status: PredictionStatus,
    val mainDisease: DiseaseType?,
    val confidence: Float?,
    val detections: List<DetectionDto>?
)
