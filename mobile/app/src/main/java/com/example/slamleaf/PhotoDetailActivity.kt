package com.example.slamleaf

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.slamleaf.data.remote.DetectionDto
import com.google.gson.Gson
import java.io.File


class PhotoDetailActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var textDisease: TextView
    private lateinit var textConfidence: TextView
    private lateinit var progressConfidenceDetail: ProgressBar
    private lateinit var textDescription: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_detail)

        imageView = findViewById(R.id.imageDetail)
        textDisease = findViewById(R.id.textDisease)
        textConfidence = findViewById(R.id.textConfidence)
        progressConfidenceDetail = findViewById(R.id.progressConfidenceDetail)
        textDescription = findViewById(R.id.textDescription)

        val localPath = intent.getStringExtra("localPath")
        val processedUrl = intent.getStringExtra("processedUrl")
        val diseaseName = intent.getStringExtra("diseaseName") ?: "Brak rozpoznania"
        val confidence = intent.getFloatExtra("confidence", -1f)

        val detectionsJson = intent.getStringExtra("detectionsJson")
        val detections: List<DetectionDto> = if (detectionsJson != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<DetectionDto>>() {}.type
            Gson().fromJson(detectionsJson, type)
        } else emptyList()

        val mainDiseaseType : DiseaseType? =
        detections.maxByOrNull { it.confidence }
            ?.let { DiseaseType.fromClassId(it.class_id) }

        // jeśli nie ma predykcji - lokalny plik
        val toLoad = processedUrl ?: localPath
        if (toLoad != null) {
            if (toLoad.startsWith("http")) {
                Glide.with(this)
                    .load(toLoad)
                    .into(imageView)
            } else {
                Glide.with(this)
                    .load(File(toLoad))
                    .into(imageView)
            }
        }

        textDisease.text = diseaseName
        textDescription.text = mainDiseaseType?.let {getDescriptionForDisease(it)}
            ?: "Brak opisu choroby"


        if (confidence >= 0f) {
            val percent = (confidence * 100).toInt()
            textConfidence.text = "Pewność: $percent %"
            progressConfidenceDetail.max = 100
            progressConfidenceDetail.progress = percent

            val colorInt = when {
                confidence >= 0.85f -> 0xFF2E7D32.toInt() // zielony
                confidence >= 0.6f  -> 0xFFF9A825.toInt() // żółty
                else          -> 0xFFC62828.toInt() // czerwony
            }

            progressConfidenceDetail.progressTintList =
                android.content.res.ColorStateList.valueOf(colorInt)
        } else {
            textConfidence.text = "Pewność: brak danych"
            progressConfidenceDetail.progress = 0
        }
        showRecommendedProducts(mainDiseaseType)

        showOtherDetections(detections, mainDiseaseType)
    }


    private fun showOtherDetections(
        allDetections: List<DetectionDto>,
        mainDiseaseType: DiseaseType?
    ) {
        val container = findViewById<LinearLayout>(R.id.containerOtherDetections)
        container.removeAllViews()

        if (allDetections.isEmpty()) return

        // grupujemy po klasie
        val grouped = allDetections.groupBy { it.class_id }

        grouped.forEach { (classId, groupList) ->
            val diseaseType = DiseaseType.fromClassId(classId)

            // pomijamy główną chorobę jest wyżej w głównej karcie
            if (diseaseType == mainDiseaseType) return@forEach

            val avgConf = groupList.map { it.confidence }.max().toFloat()
            val recognitionsCount = groupList.size

            val itemView = layoutInflater.inflate(
                R.layout.item_other_detection,
                container,
                false
            )

            val nameView = itemView.findViewById<TextView>(R.id.textDiseaseName)
            val recView = itemView.findViewById<TextView>(R.id.textRecognitions)
            val confBar = itemView.findViewById<ProgressBar>(R.id.progressConfidenceOther)
            val confText = itemView.findViewById<TextView>(R.id.textConfidenceOther)
            val descView = itemView.findViewById<TextView>(R.id.textDescriptionOther)

            nameView.text = diseaseType.displayName
            recView.text = "Ilość rozpoznań: $recognitionsCount"

            val percent = (avgConf * 100).toInt()
            confBar.max = 100
            confBar.progress = percent
            confText.text = "$percent%"

            val colorInt = when {
                avgConf >= 0.85f -> 0xFF2E7D32.toInt()
                avgConf >= 0.6f  -> 0xFFF9A825.toInt()
                else             -> 0xFFC62828.toInt()
            }
            confBar.progressTintList =
                android.content.res.ColorStateList.valueOf(colorInt)

            descView.text = getDescriptionForDisease(diseaseType)
            container.addView(itemView)
        }
    }

    private fun showRecommendedProducts(mainDiseaseType: DiseaseType?) {
        val card = findViewById<androidx.cardview.widget.CardView>(R.id.cardRecommendedProducts)
        val container = findViewById<LinearLayout>(R.id.containerRecommendedProducts)
        val noText = findViewById<TextView>(R.id.textNoRecommended)

        container.removeAllViews()

        if (mainDiseaseType == null) {
            card.visibility = android.view.View.GONE
            return
        } else {
            card.visibility = android.view.View.VISIBLE
        }

        val products = getRecommendedProductsForDisease(mainDiseaseType)

        if (products.isEmpty()) {
            noText.visibility = android.view.View.VISIBLE
            return
        } else {
            noText.visibility = android.view.View.GONE
        }

        products.take(3).forEach { (name, substance, kind) ->
            // mini-card (rounded, light green)
            val miniCard = androidx.cardview.widget.CardView(this).apply {
                radius = dpF(14f)
                cardElevation = dpF(0f)
                setCardBackgroundColor(0xFFE6F4EE.toInt()) // jasna zieleń jak na screenie

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(14)
                layoutParams = lp
            }

            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(16), dp(18), dp(16))
            }

            val nameTv = TextView(this).apply {
                text = name
                textSize = 22f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(0xFF176F3B.toInt()) // ciemna zieleń
            }

            val substanceTv = TextView(this).apply {
                text = "substancja czynna: $substance"
                textSize = 18f
                setPadding(0, dp(8), 0, 0)
                setTextColor(0xFF6B7280.toInt()) // szary
            }

            val kindTv = TextView(this).apply {
                text = "rodzaj środka: $kind"
                textSize = 18f
                setPadding(0, dp(6), 0, 0)
                setTextColor(0xFF6B7280.toInt()) // szary
            }

            inner.addView(nameTv)
            inner.addView(substanceTv)
            inner.addView(kindTv)

            miniCard.addView(inner)
            container.addView(miniCard)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dpF(value: Float): Float =
        value * resources.displayMetrics.density

    private fun getRecommendedProductsForDisease(disease: DiseaseType): List<Triple<String, String, String>> =
        when (disease) {

            DiseaseType.APPLE_SCAB -> listOf(
                Triple("Score 250 EC", "difenokonazol", "fungicyd"),
                Triple("Zato 50 WG", "trifloksystrobina", "fungicyd"),
                Triple("Delan 700 WG", "ditianon", "fungicyd")
            )

            DiseaseType.APPLE_RUST -> listOf(
                Triple("Score 250 EC", "difenokonazol", "fungicyd"),
                Triple("Domark 100 EC", "tetrakonazol", "fungicyd"),
                Triple("Zato 50 WG", "trifloksystrobina", "fungicyd")
            )

            DiseaseType.CORN_RUST -> listOf(
                Triple("Insignia", "piraklostrobina ", "fungicyd"),
                Triple("Retengo", "piraklostrobina", "fungicyd"),
                Triple("Retengo Plus", "piraklostrobina + epoksykonazol", "fungicyd")
            )


            DiseaseType.CORN_GRAY_LEAF_SPOT -> listOf(
                Triple("Propulse 250 SE", "protiokonazol  + fluopyram ", "fungicyd"),
                Triple("Belavent", "mefentriflukonazol ", "fungicyd"),
                Triple("Zetar 250 SC", "azoksystrobina", "fungicyd")
            )

            DiseaseType.CORN_LEAF_BLIGHT -> listOf(
                Triple("Opera Max", "piraklostrobina + epoksykonazol", "fungicyd"),
                Triple("Amistar 250 SC", "azoksystrobina", "fungicyd"),
                Triple("Elatus Era", "benzowindiflupyr + azoksystrobina", "fungicyd")
            )

            DiseaseType.POTATO_EARLY_BLIGHT -> listOf(
                Triple("Signum 33 WG", "boskalid + piraklostrobina", "fungicyd"),
                Triple("Amistar 250 SC", "azoksystrobina", "fungicyd"),
                Triple("Globdif", "difenokonazol", "fungicyd")
            )

            DiseaseType.POTATO_LATE_BLIGHT -> listOf(
                Triple("Ridomil Gold MZ", "metalaksyl-M + mankozeb", "fungicyd"),
                Triple("Infinito 687,5 SC", "fluopikolid + propamokarb", "fungicyd"),
                Triple("Revus 250 SC", "mandipropamid", "fungicyd")
            )

            DiseaseType.TOMATO_EARLY_BLIGHT -> listOf(
                Triple("Signum 33 WG", "boskalid + piraklostrobina", "fungicyd"),
                Triple("Amistar 250 SC", "azoksystrobina", "fungicyd"),
                Triple("Score 250 EC", "difenokonazol", "fungicyd")
            )

            DiseaseType.TOMATO_BACTERIAL_SPOT -> listOf(
                Triple("Miedzian 50 WP", "tlenochlorek miedzi", "fungicyd/bakteriocyd"),
                Triple("Miedzian Extra 350 SC", "wodorotlenek miedzi", "fungicyd/bakteriocyd"),
                Triple("Fungistar", "azoksystrobina ", "fungicyd")
            )

            DiseaseType.TOMATO_LATE_BLIGHT -> listOf(
                Triple("Revus 250 SC", "mandipropamid", "fungicyd"),
                Triple("Singapur 33 WG", "boskalid  + piraklostrobina ", "fungicyd"),
                Triple("Ridomil Gold MZ", "metalaksyl-M + mankozeb", "fungicyd")
            )

            DiseaseType.TOMATO_MOSAIC_VIRUS,
            DiseaseType.TOMATO_YELLOW_VIRUS -> emptyList()

            DiseaseType.TOMATO_MOLD_LEAF -> listOf(
                Triple("Switch 62,5 WG", "cyprodynil + fludioksonil", "fungicyd"),
                Triple("Teldor 500 SC", "fenheksamid", "fungicyd"),
                Triple("Signum 33 WG", "boskalid + piraklostrobina", "fungicyd")
            )

            DiseaseType.SQUASH_POWDERY_MILDEW -> listOf(
                Triple("Romeo", "cerewisan ", "fungicyd"),
                Triple("Mag 50 WG", "miedź w postaci wodorotlenku miedz", "bakteriocyd/fungicyd"),
                Triple("Champion 50 WG", "miedź w postaci wodorotlenku miedzi", "fungicyd")
            )
            DiseaseType.GRAPE_BLACK_ROT -> listOf(
                Triple("Karpo", "mefentriflukonazol ", "fungicyd"),
                Triple("Revyona", "mefentriflukonazol ", "fungicyd"),
                Triple("Tamin 100 EC", "penkonazol", "fungicyd")
            )

            else -> emptyList()
        }


    private fun getDescriptionForDisease(disease: DiseaseType): String =
        when (disease) {

            DiseaseType.APPLE_SCAB ->
                "Parch jabłoni to choroba grzybowa powodująca ciemne plamy na liściach i owocach. Prowadzi do osłabienia drzewa i obniżenia jakości plonów. Pomaga usuwanie porażonych liści oraz stosowanie fungicydów i odmian odpornych."

            DiseaseType.APPLE_RUST ->
                "Rdza jabłoni objawia się pomarańczowymi plamami na liściach i deformacją pędów. Choroba wymaga obecności jałowca jako żywiciela pośredniego. Pomaga eliminacja jałowców w pobliżu oraz opryski fungicydowe."

            DiseaseType.CORN_GRAY_LEAF_SPOT ->
                "Szara plamistość liści kukurydzy powoduje podłużne, szare zmiany nekrotyczne. Obniża intensywność fotosyntezy i plon. Zalecana jest rotacja upraw, odporne odmiany oraz ochrona chemiczna."

            DiseaseType.CORN_LEAF_BLIGHT ->
                "Zaraza liści kukurydzy to choroba grzybowa powodująca brunatne plamy i przedwczesne zamieranie liści. Może znacząco obniżyć plon ziarna. Pomaga stosowanie zdrowego materiału siewnego i fungicydów."

            DiseaseType.CORN_RUST ->
                "Rdza kukurydzy objawia się rdzawymi pustulami na powierzchni liści. Choroba osłabia roślinę i skraca okres wegetacji. Skuteczne są odmiany odporne oraz zabiegi fungicydowe."

            DiseaseType.POTATO_EARLY_BLIGHT ->
                "Alternarioza ziemniaka powoduje brunatne, koncentryczne plamy na liściach. Choroba rozwija się intensywnie w warunkach suszy i wysokiej temperatury. Pomaga prawidłowe nawożenie i ochrona fungicydowa."

            DiseaseType.POTATO_LATE_BLIGHT ->
                "Zaraza ziemniaka to bardzo groźna choroba grzybowa prowadząca do szybkiego zamierania roślin. Rozwija się w wilgotnych i chłodnych warunkach. Wymaga regularnych oprysków oraz usuwania porażonych części."

            DiseaseType.TOMATO_EARLY_BLIGHT ->
                "Alternarioza pomidora objawia się ciemnymi plamami z koncentrycznymi pierścieniami. Choroba osłabia roślinę i ogranicza plon. Pomaga rotacja upraw oraz stosowanie fungicydów."

            DiseaseType.TOMATO_SEPTORIA ->
                "Septorioza liści pomidora powoduje drobne, jasne plamy z ciemną obwódką. Prowadzi do przedwczesnego opadania liści. Zaleca się usuwanie porażonych liści i stosowanie środków ochrony roślin."

            DiseaseType.TOMATO_BACTERIAL_SPOT ->
                "Bakteryjna plamistość pomidora powoduje ciemne, wodniste plamy na liściach i owocach. Choroba szybko rozprzestrzenia się w wilgotnych warunkach. Pomaga stosowanie zdrowych nasion i ograniczenie wilgotności."

            DiseaseType.TOMATO_LATE_BLIGHT ->
                "Zaraza ziemniaka na pomidorze prowadzi do gwałtownego zamierania liści i owoców. Choroba rozwija się przy wysokiej wilgotności powietrza. Zalecana jest szybka ochrona fungicydowa."

            DiseaseType.TOMATO_MOSAIC_VIRUS ->
                "Wirus mozaiki pomidora powoduje mozaikowe przebarwienia i deformacje liści. Choroba jest nieuleczalna i przenosi się mechanicznie. Zaleca się usuwanie porażonych roślin."

            DiseaseType.TOMATO_YELLOW_VIRUS ->
                "Wirus żółtej kędzierzawości liści pomidora prowadzi do żółknięcia, kędzierzawości i redukcji powierzchni asymilacyjnej liści. Skutkuje zahamowaniem wzrostu oraz znacznym obniżeniem plonu. Ograniczanie choroby opiera się na kontroli populacji mączlików będących wektorem wirusa."

            DiseaseType.TOMATO_MOLD_LEAF ->
                "Pleśń liści pomidora objawia się żółtymi plamami na górnej stronie liścia i nalotem od spodu. Choroba rozwija się w wysokiej wilgotności. Pomaga wietrzenie upraw i stosowanie fungicydów."

            DiseaseType.TOMATO_SPIDER_MITES ->
                "Przędziorek chmielowiec to szkodnik powodujący żółknięcie i zasychanie liści. Na spodniej stronie widoczna jest delikatna pajęczyna. Pomaga zwalczanie akarycydami i zwiększenie wilgotności powietrza."

            DiseaseType.GRAPE_BLACK_ROT ->
                "Czarna zgnilizna winorośli powoduje ciemnienie i gnicie liści oraz owoców. Choroba sprzyja ciepła i wilgotna pogoda. Zalecane są opryski fungicydowe i usuwanie porażonych części."

            DiseaseType.SQUASH_POWDERY_MILDEW ->
                "Mączniak prawdziwy dyniowatych objawia się białym, mączystym nalotem na liściach. Ogranicza fotosyntezę i wzrost rośliny. Pomaga dobra wentylacja i stosowanie fungicydów."

            DiseaseType.UNKNOWN ->
                "Nie udało się jednoznacznie rozpoznać choroby. Zdjęcie może być nieczytelne lub choroba nie znajduje się w bazie systemu. Zaleca się wykonanie nowego zdjęcia w lepszych warunkach."

            else ->
                "Roślina wygląda na zdrową. Nie wykryto objawów choroby na podstawie dostarczonego obrazu."
        }



}
