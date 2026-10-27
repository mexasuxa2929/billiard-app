# 🎱 Billiard Club Manager — KMP Desktop + Room

Bilyard xonasini boshqarish uchun desktop app.

**Texnologiyalar:** Kotlin Multiplatform (shared + desktopApp), Compose Multiplatform Desktop (Material3, dark theme), **Room KMP 2.8.3** + `sqlite-bundled` (JVM desktop), Coroutines + StateFlow, kotlinx-datetime.

**Baza fayli:** `~/.billiardapp/billiard_club.db` (bitta ko'chiriladigan `.db` fayl)
**Cheklar:** `~/.billiardapp/cheklar/`

## Funksiyalar

### 🎱 Stollar
- Stollar soni cheksiz: qo'shish / tahrirlash / o'chirish, har bir stolga alohida soatlik narx.
- `Boshlash` → jonli taymer (1 soniyada yangilanadi) + hozirgi summa.
- Hisoblash: **daqiqabay, ceil** — `cost = hourlyPrice * ceil(sec/60) / 60`, min 1 daqiqa.
- `+ Bar` → ochiq sessiyaga mahsulot yozish (stok avtomatik kamayadi, yetmasa xato).
- `🍺 Zakazlar (N)` → stolga yozilgan barcha bar zakazlarini ko'rish, o'chirish yoki yana qo'shish.
- `Yopish` → chegirma + to'lov turi (Naqd/Karta/Click) → **chek** generatsiya.
- `Bekor qilish` → stokni qaytaradi.

### 🍺 Bar
- Mahsulot: nomi, kategoriya, **kirish narxi**, **sotish narxi**, qoldiq, birlik.
- Qidiruv, kirim (`+` omborga), tahrirlash, o'chirish.
- `To'g'ridan-to'g'ri savdo` — stolsiz mijozga sotish (alohida chek + jurnal).

### 💸 Xarajatlar
- Xarajat qo'shish: **sababi** (majburiy), kategoriya, summa, to'lov turi, sana va izoh.
- Kategoriyalar: Oylik, Ijara, Kommunal, Ta'mirlash, Mahsulot, Transport, Boshqa.
- Ro'yxatda qidiruv, kategoriya bo'yicha filtr, tahrirlash va o'chirish.
- Kunlik / oylik / umumiy xarajat summasi ko'rsatiladi.

### 📊 Hisobotlar
- **Kunlik / Haftalik / Oylik / Jami** tablar.
- Kartalar: Stol daromadi, Bar savdo, Bar foydasi (sotish−kirish), JAMI daromad,
  Xarajatlar va Sof foyda.
- Sessiyalar soni, chegirmalar, TOP-10 mahsulot, cheklar ro'yxati.

**Hisobot qoidasi (chalkashmasligi uchun):**
- **Stollar** kartasi — faqat vaqt puli (`tableSum`).
- **Bar savdo/foyda** — barcha bar savdosi: to'g'ridan-to'g'ri savdo **va** stolga biriktirilgan savdo.
  Stoldagi savdo faqat **stol yopilganda** hisobga olinadi (to'lov o'sha paytda olinadi),
  bekor qilingan sessiyalarning savdosi esa hisobotga kirmaydi.
- **JAMI daromad = Stollar + Bar savdo − chegirma.** Stol kartasidagi jami summada bar bo'lsa ham,
  u bar qatorida bir marta hisoblanadi — ikki marta qo'shilmaydi.
- **Sof foyda = JAMI daromad − xarajatlar.**
- Hisobotda "stolga" va "alohida" savdo yorliqlari ko'rsatiladi, Excelda ham shu taqsimot bor.

### 🧾 Chek
- Chiroyli matnli chek: klub nomi, stol, davomiylik, tarif, bar pozitsiyalari, chegirma, JAMI.
- **Chop etish** (PrinterJob dialog) + **faylga saqlash** (.txt).

### 🧾 Tarix / Jurnal
- Barcha harakatlar `logs` jadvalida: TABLE_START/STOP, BAR_SALE, PRODUCT, EXPENSE, SYSTEM — summa bilan.

### ⚙️ Sozlamalar
- Klub nomi, standart soatlik narx. Birinchi ishga tushishda 6 ta stol + 4 demo mahsulot avtomatik yaratiladi.
- **Portable baza:** joriy `.db` yo'lini ko'rish, boshqa `.db` tanlash, izchil nusxa saqlash, standart joyga qaytish.

## Portable baza (`.db` ni ko'chirish)

Butun baza — **bitta `.db` fayl**. WAL rejimi o'rniga `TRUNCATE` ishlatiladi, shuning uchun
ishdan keyin `-wal` / `-shm` yon fayllari qolmaydi.

**Boshqa kompyuterga / papkaga ko'chirish:**
1. `.db` faylni nusxalang (masalan USB yoki tarmoq orqali).
2. Yangi kompyuterda: **Sozlamalar → 💾 Ma'lumotlar bazasi → 📂 Boshqa .db tanlash**.
3. Dasturni qayta ishga tushiring — barcha ma'lumotlar shu fayldan o'qiladi.

Fayl `~/.billiardapp/billiard_club.db` ustiga ko'chirilsa, hech narsa tanlash shart emas.

**Nusxa (backup):** Sozlamalardagi `💾 Nusxa saqlash` `VACUUM INTO` bilan izchil nusxa oladi —
dastur ishlab turgan paytda ham xavfsiz. Standart papka: `~/.billiardapp/backup/`.

**Yo'lni qo'lda berish** (konfiguratsiya faylidan ustun turadi):
```bash
BILLIARD_DB=/home/user/baza.db ./gradlew :desktopApp:run
# yoki
./gradlew :desktopApp:run -Dbilliard.db=/home/user/baza.db
```

Tanlangan yo'l `~/.billiardapp/db_path.txt` da saqlanadi. Fayl topilmasa yoki o'qilmasa,
dastur xavfsiz tarzda standart joyga qaytadi. Tanlashdan oldin fayl tekshiriladi:
SQLite sarlavhasi, `PRAGMA user_version` (1..4) va `tables` jadvali mavjudligi.

## Loyiha strukturasi

```
shared/src/commonMain/.../data/entity/Entities.kt   — 11 ta Room entity
shared/src/commonMain/.../data/dao/Daos.kt          — 10 ta DAO (Flow + suspend)
shared/src/commonMain/.../data/BilliardDatabase.kt  — RoomDatabase
shared/src/commonMain/.../data/DatabaseBuilder.kt   — expect + BundledSQLiteDriver (TRUNCATE)
shared/src/jvmMain/.../data/DatabaseBuilder.jvm.kt  — DatabaseLocation.activePath() (actual)
shared/src/commonMain/.../data/DatabaseLocation.kt  — portable `.db` yo'li (expect)
shared/src/jvmMain/.../data/DatabaseLocation.jvm.kt — yo'l, tekshirish, VACUUM INTO nusxa
shared/src/commonMain/.../data/repository/         — barcha biznes operatsiyalar
shared/src/commonMain/.../domain/Billing.kt         — tarif, format, report, check text
shared/src/commonMain/.../ui/screens/              — Tables, Bar, Reports, History, Settings
shared/src/commonMain/.../ui/viewmodel/            — AppViewModel (yagona state)
desktopApp/src/main/.../main.kt                     — Window 1280x800 + DB init
```

## Moslashuvchan UI (responsive)

- Oyna o'lchamiga qarab 3 rejim ishlaydi: **Compact** (kontent < 760dp), **Medium** (760–1040dp), **Expanded** (>= 1040dp).
- Tor oynada yon menyu ikonkalar paneliga (rail) aylanadi, keng jadvallar kartalar ko'rinishiga o'tadi, filtrlar/tugmalar keyingi qatorga tushadi.
- Hisobotlarda 4 ta karta tor ekranda 2×2 ko'rinishida, pastki bloklar ustma-ust joylashadi.
- Eng kichik oyna o'lchami: **780×560** (`main.kt` dagi `window.minimumSize`).

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
