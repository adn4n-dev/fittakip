# FitTakip

Kalori hesaplama, yemek günlüğü, antrenman notları ve gelişim tahmini yapan Android uygulaması.
Kotlin + Jetpack Compose (Material 3) + Room. Tüm veriler telefonda, internet gerekmez.

## Çalıştırma

1. Android Studio'yu aç → **Open** → `FitTakip` klasörünü seç.
2. Gradle senkronizasyonu bitsin (ilk seferde bağımlılıklar inecek, birkaç dakika sürer).
3. Telefonu USB hata ayıklama ile bağla ya da emülatör aç → ▶ **Run**.

Gereksinim: Android Studio Ladybug (2024.2) veya daha yeni, JDK 17. Telefon Android 8.0+.

## Sekmeler (2.0)

| Sekme | Ne yapıyor |
|---|---|
| **Bugün** | Günlük seri ve haftanın günleri, seviye ve XP, 4 günlük görev, hızlı özet, haftalık antrenman hedefi, son rozetler, günün sözü. Üstten başarımlar, haftalık rapor ve ayarlar. |
| **Beslenme** | Kalori halkası ve makrolar, su takibi, 80+ hazır Türk yemeği ile arama, porsiyon çarpanı, öğünler, sık yenenler, dünü kopyala, geçmiş günler, son 7 gün grafiği. |
| **Antrenman** | Hazır programlar (Push/Pull/Legs…), 38 hareketlik kütüphane, son değerlerle otomatik doldurma, ağırlık önerisi, dinlenme sayacı, rekor uyarısı ve kutlaması, kardiyo, plaka hesaplayıcı, 1RM yüzde tablosu. |
| **Gelişim** | Antrenman takvimi (ısı haritası), kas gruplarına haftalık hacim, toplam rekor ve tonaj, hareket bazında gelişim tahmini. |
| **Vücut** | Trend kilo, hedefe ilerleme ve varış tarihi, tek dokunuşla kalori ayarı, BMI, vücut yağ oranı, mezura ölçüleri. |

Bildirim: o gün kayıt yoksa akşam seri hatırlatması. Ayarlar: hedefler, dinlenme süresi, JSON yedekleme ve geri yükleme.

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
