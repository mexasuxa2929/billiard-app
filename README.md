# 🎱 Billiard Club Manager — KMP Desktop + Room

Bilyard xonasini boshqarish uchun desktop app.

**Texnologiyalar:** Kotlin Multiplatform (shared + desktopApp), Compose Multiplatform Desktop (Material3, dark theme), **Room KMP 2.8.3** + `sqlite-bundled` (JVM desktop), Coroutines + StateFlow, kotlinx-datetime.

**Baza fayli:** `~/.billiardapp/billiard_club.db`
**Cheklar:** `~/.billiardapp/cheklar/`

## Funksiyalar

### 🎱 Stollar
- Stollar soni cheksiz: qo'shish / tahrirlash / o'chirish, har bir stolga alohida soatlik narx.
- `Boshlash` → jonli taymer (1 soniyada yangilanadi) + hozirgi summa.
- Hisoblash: **daqiqabay, ceil** — `cost = hourlyPrice * ceil(sec/60) / 60`, min 1 daqiqa.
- `+ Bar` → ochiq sessiyaga mahsulot yozish (stok avtomatik kamayadi, yetmasa xato).
- `Yopish` → chegirma + to'lov turi (Naqd/Karta/Click) → **chek** generatsiya.
- `Bekor qilish` → stokni qaytaradi.

### 🍺 Bar
- Mahsulot: nomi, kategoriya, **kirish narxi**, **sotish narxi**, qoldiq, birlik.
- Qidiruv, kirim (`+` omborga), tahrirlash, o'chirish.
- `To'g'ridan-to'g'ri savdo` — stolsiz mijozga sotish (alohida chek + jurnal).

### 📊 Hisobotlar
- **Kunlik / Haftalik / Oylik / Jami** tablar.
- Kartalar: Stol daromadi, Bar savdo, Bar foydasi (sotish−kirish), JAMI.
- Sessiyalar soni, chegirmalar, TOP-10 mahsulot, cheklar ro'yxati.

### 🧾 Chek
- Chiroyli matnli chek: klub nomi, stol, davomiylik, tarif, bar pozitsiyalari, chegirma, JAMI.
- **Chop etish** (PrinterJob dialog) + **faylga saqlash** (.txt).

### 🧾 Tarix / Jurnal
- Barcha harakatlar `logs` jadvalida: TABLE_START/STOP, BAR_SALE, PRODUCT, SYSTEM — summa bilan.

### ⚙️ Sozlamalar
- Klub nomi, standart soatlik narx. Birinchi ishga tushishda 6 ta stol + 4 demo mahsulot avtomatik yaratiladi.

## Loyiha strukturasi

```
shared/src/commonMain/.../data/entity/Entities.kt   — 8 ta Room entity
shared/src/commonMain/.../data/dao/Daos.kt          — 7 ta DAO (Flow + suspend)
shared/src/commonMain/.../data/BilliardDatabase.kt  — RoomDatabase
shared/src/commonMain/.../data/DatabaseBuilder.kt   — expect + BundledSQLiteDriver
shared/src/jvmMain/.../data/DatabaseBuilder.jvm.kt  — ~/.billiardapp path (actual)
shared/src/commonMain/.../data/repository/         — barcha biznes operatsiyalar
shared/src/commonMain/.../domain/Billing.kt         — tarif, format, report, check text
shared/src/commonMain/.../ui/screens/              — Tables, Bar, Reports, History, Settings
shared/src/commonMain/.../ui/viewmodel/            — AppViewModel (yagona state)
desktopApp/src/main/.../main.kt                     — Window 1280x800 + DB init
```

## Ishga tushirish

```bash
./gradlew :desktopApp:run
# yoki hot reload:
./gradlew :desktopApp:hotRun --auto
```

Distributiv (msi/deb/dmg):
```bash
./gradlew :desktopApp:createDistributable
./gradlew :desktopApp:packageDistributionForCurrentOS
```

## Windows setup (.exe / .msi) yasash

Muhim: Windows installer **faqat Windows da** yig'iladi (jpackage + WiX talabi —
Linux da MSI/EXE qilib bo'lmaydi). 2 ta yo'l:

**1-usul — Windows kompyuterda (JDK 17+ o'rnatilgan bo'lsin):**
```bat
cd BilliardApp
gradlew.bat :desktopApp:packageMsi
gradlew.bat :desktopApp:packageExe
```
Tayyor fayllar:
- `desktopApp\build\compose\binaries\main\msi\BilliardClubManager-2.4.0.msi`
- `desktopApp\build\compose\binaries\main\exe\BilliardClubManager-2.4.0.exe`

O'rnatishda papka tanlash oynasi chiqadi, Start menyuga yorliq qo'shiladi.
Keyingi versiyalar ustiga o'rnatiladi (`upgradeUuid` qotirilgan — uni o'zgartirmang).

**2-usul — GitHub Actions (Windows sizda bo'lmasa):**
Repo ni GitHub ga push qiling → Actions → `Windows installer (MSI + EXE)` →
`Run workflow` → tugagach `windows-msi` / `windows-exe` artifact ni yuklab oling.
`v1.0` kabi tag push qilinsa avtomatik ishga tushadi.

Test:
```bash
./gradlew :shared:jvmTest
```

## Muhim Gradle eslatma
- Kotlin `2.3.10` + KSP `2.3.10` + Room `2.8.3` + SQLite `2.7.1` — KSP hali Kotlin 2.4 ni qo'llamagani uchun atay 2.3.10 ga tushirilgan.
- `shared/build.gradle.kts` da `kspJvm(room-compiler)` va `room { schemaDirectory }` shart.
