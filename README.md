# FitTakip

Kalori hesaplama, yemek günlüğü, antrenman notları ve gelişim tahmini yapan Android uygulaması.
Kotlin + Jetpack Compose (Material 3) + Room. Tüm veriler telefonda, internet gerekmez.

## Çalıştırma

1. Android Studio'yu aç → **Open** → `FitTakip` klasörünü seç.
2. Gradle senkronizasyonu bitsin (ilk seferde bağımlılıklar inecek, birkaç dakika sürer).
3. Telefonu USB hata ayıklama ile bağla ya da emülatör aç → ▶ **Run**.

Gereksinim: Android Studio Ladybug (2024.2) veya daha yeni, JDK 17. Telefon Android 8.0+.

## Sekmeler

| Sekme | Ne yapıyor |
|---|---|
| **Kalori** | Profil (cinsiyet, yaş, boy, kilo, aktivite, hedef) → BMR, TDEE, günlük kalori hedefi ve makrolar. Gün içinde yediklerini ekle; kalan kalori ve makro çubukları. Kalori boş bırakılırsa protein/karb/yağdan hesaplanır. |
| **Antrenman** | Hareket + kg + tekrar + not. Önceki hareketler öneri olarak çıkar; hareket ve kg kayıttan sonra kalır, setleri hızlı girersin. Güne göre gruplu geçmiş. |
| **Gelişim** | Her hareket için: en iyi tahmini 1RM, haftalık % değişim, 4 hafta sonrası tahmini, güvenilirlik ve grafik. Sonuç: *Gelişiyorsun / Plato / Düşüş*. |
| **Kilo** | Günlük tartı, yumuşatılmış "trend kilo", haftalık değişim, hedefe göre yorum ve hedef kiloya tahmini varış tarihi. Tartı girince profildeki kilo ve kalori hedefi de güncellenir. |

## Tahmin nasıl çalışıyor?

**Güç gelişimi** (`domain/Progress.kt`)
- Her antrenman gününde o hareketteki en iyi setten tahmini 1RM: Epley → `kg × (1 + tekrar/30)`. Böylece 80×8 ile 90×5 karşılaştırılabilir.
- Son 8 haftanın verisine en küçük kareler ile doğrusal regresyon.
- Eğim → haftalık % değişim. `> +%0,5` gelişim, `< −%0,5` düşüş, arası plato.
- Doğru 4 hafta ileri uzatılarak tahmin yapılır; R² ve gün sayısıyla güvenilirlik (Düşük/Orta/Yüksek).
- En az 3 farklı antrenman günü gerekir.

**Kilo trendi**
- Aynı gün birden çok tartı varsa ortalaması alınır, üstel hareketli ortalama (α = 0,3) ile günlük su dalgalanması bastırılır.
- Son 4 haftaya regresyon → kg/hafta. Hedef kilo varsa ve doğru yöndeysen `kalan kg ÷ günlük eğim` ile varış tarihi.
- Diyet hedefinde ideal tempo −0,2…−1 kg/hafta, bulk'ta +0,1…+0,6 kg/hafta kabul edilir.

**Kalori**: Mifflin-St Jeor BMR × aktivite katsayısı. Diyet −500, bulk +300 kcal. Taban: erkek 1500, kadın 1200 kcal. Protein diyette 2,2 g/kg, diğerlerinde 1,8 g/kg; yağ kalorinin %25'i; kalan karbonhidrat.

Eşikleri değiştirmek için `ProgressAnalyzer` içindeki sabitlere (`PLATEAU_THRESHOLD_PCT`, `STRENGTH_WINDOW_DAYS`, `WEIGHT_WINDOW_DAYS`) bak.

## Testler

```
./gradlew test
```
`app/src/test/.../ProgressAnalyzerTest.kt` regresyon, 1RM, gelişim/plato kararları ve kilo tahminini test eder.

## Proje yapısı

```
app/src/main/java/com/fittakip/app/
├── MainActivity.kt          alt menü + sekmeler
├── data/                    Room: tablolar, DAO, veritabanı
├── domain/
│   ├── Calculator.kt        BMR / TDEE / makro
│   └── Progress.kt          regresyon, 1RM, gelişim ve kilo tahmini
└── ui/                      ViewModel, ekranlar, grafik, tema
```

## Sonraki adımlar için fikirler

- Barkod okuyup Open Food Facts'ten besin değeri çekme
- Sık yenen yemekleri favorilere kaydetme
- Antrenman şablonları (Push/Pull/Legs) ve dinlenme sayacı
- Haftalık hacim (set × tekrar × kg) takibi
- Verileri JSON/CSV olarak dışa aktarma
