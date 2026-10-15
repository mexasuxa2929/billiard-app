package org.mexa.clube.data

/**
 * Ma'lumotlar bazasining joylashuvini boshqarish.
 *
 * Asosiy g'oya: butun baza — bitta `.db` fayl. Uni bir papkadan boshqasiga yoki
 * boshqa kompyuterga shunchaki nusxalash yetarli. Baza yo'li bazaning o'zida emas,
 * alohida kichik konfiguratsiya faylida (`~/.billiardapp/db_path.txt`) saqlanadi.
 *
 * Yo'lni aniqlash tartibi:
 *  1. `-Dbilliard.db=/yo'l/baza.db` (JVM tizim xususiyati)
 *  2. `BILLIARD_DB=/yo'l/baza.db` (muhit o'zgaruvchisi)
 *  3. `~/.billiardapp/db_path.txt` (Sozlamalar oynasidan tanlangan yo'l)
 *  4. Standart: `~/.billiardapp/billiard_club.db`
 */
expect object DatabaseLocation {

    /** Standart papka: `~/.billiardapp` */
    fun defaultDir(): String

    /** Standart baza fayli: `~/.billiardapp/billiard_club.db` */
    fun defaultPath(): String

    /** Sozlamalarda saqlangan yo'l (bo'lmasa null). */
    fun configuredPath(): String?

    /**
     * Qayta ishga tushirilgandan keyin ishlatiladigan yo'l — faqat fayl mavjud,
     * o'qiladigan va hozirgisidan farq qilsa qaytariladi.
     */
    fun pendingPath(): String?

    /** Hozir amalda ishlatilayotgan baza yo'li. */
    fun activePath(): String

    /**
     * `.db` faylni tekshiradi.
     * @return `null` — fayl yaroqli; aks holda foydalanuvchiga ko'rsatiladigan xato matni.
     */
    fun validate(path: String): String?

    /** Yangi baza yo'lini saqlaydi. Yaroqsiz bo'lsa [IllegalStateException] tashlaydi. */
    fun usePath(path: String)

    /** Standart yo'lga qaytaradi (konfiguratsiyani o'chiradi). */
    fun resetToDefault()

    /** Nusxalar uchun standart papka: `~/.billiardapp/backup` */
    fun backupDir(): String

    /**
     * Bazaning izchil (consistent) nusxasini yaratadi va to'liq yo'lni qaytaradi.
     * `VACUUM INTO` ishlatiladi — dastur ishlab turgan paytda ham xavfsiz.
     */
    fun backup(sourcePath: String, targetDir: String): String
}
