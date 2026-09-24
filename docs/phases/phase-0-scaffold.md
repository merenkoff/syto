# Фаза 0 — Каркас и конвейер

Статус: 🔄 · Начата: 2026-09-24 · Завершена: —

## Цель

Есть Gradle-проект с пустым Compose-приложением, GitHub Actions собирает debug-APK
на каждый пуш, и этот APK ставится и запускается на тестовом телефоне. После этого
любая сессия может изменить код и через несколько минут получить его на устройстве.

## Гейт G0

APK, собранный в CI из ветки, установлен на тестовом телефоне и показывает экран
с версией и хэшем коммита. Записываем в `DECISIONS.md` модель и версию Android
тестового устройства.

## Зависимости

Нет. Нужен ответ на D-005 (пакет приложения) — получен.

## Подфазы

### 0.1 — Gradle-проект

Статус: ✅ (2026-09-24, CI run #1 зелёный)

**Цель.** Проект собирается командой `./gradlew assembleDebug`.

**Делает агент.**

- `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, Gradle wrapper.
- Модуль `app`: Kotlin 2.x, Compose (BOM), minSdk 29, targetSdk актуальный, `applicationId = com.ownnet.syto`.
- Манифест без `INTERNET`. Одна `MainActivity` с текстом: имя приложения, `versionName`, короткий хэш коммита (прокидывается через `BuildConfig` из CI или git).
- Иконка из `assets/icon/` как launcher icon.
- Проверить, можно ли в облачной сессии поставить Android SDK (cmdline-tools через `dl.google.com`). Результат записать сюда: если нельзя, локальная проверка сборки в сессии невозможна и вся проверка — в CI.

**Проверяет пользователь.** Ничего.

**DoD.** `assembleDebug` проходит хотя бы в одном окружении (сессия или CI).

**Результат.**

- 2026-09-24, облачная сессия. Написано всё из списка. Структура:
  `settings.gradle.kts` · `build.gradle.kts` · `gradle.properties` · `gradle/libs.versions.toml` ·
  `gradle/wrapper/` · `app/build.gradle.kts` · `app/src/main/{AndroidManifest.xml,kotlin/,res/}` ·
  `app/debug.keystore` (D-009) · `scripts/svg2vector.py`.
- **Android SDK в облаке поставить нельзя.** Прокси облачной сессии закрывает `dl.google.com`
  (HTTP 403 на CONNECT) — это и SDK-репозиторий, и Google Maven, откуда берутся AGP и AndroidX.
  Доступны Maven Central, `services.gradle.org`, `plugins.gradle.org`, `api.github.com`.
  Следствие: в облаке нельзя даже сконфигурировать проект (`./gradlew help` упадёт на резолве
  AGP). Проверка сборки — только в CI или локально на Mac (D-008).
- Версии инструментов (все — в `gradle/libs.versions.toml`, там же комментарии почему):

  | Что | Версия | Почему так |
  |---|---|---|
  | Gradle | 9.6.1 (wrapper, с `distributionSha256Sum`) | AGP 9.4 требует ≥ 9.6.0 |
  | AGP | 9.4.0 | актуальный стабильный на 2026-09; JDK 17; max API 37 |
  | Kotlin / Compose compiler plugin | 2.4.10 | версия из официального гайда по Compose; 2.4.20 вышла на днях, обновим позже |
  | Compose BOM | 2026.09.00 | UI 1.12.1, Material3 1.4.0. **Compose 1.12 требует compileSdk 37 и AGP 9** |
  | compileSdk / targetSdk | 37 | требование Compose 1.12; на раннере GitHub есть `android-37.0` и Build-Tools 37.0.0 / 36.0.0 |
  | minSdk | 29 | по README |
  | activity-compose | 1.13.0 | не входит в BOM |

- **AGP 9 = built-in Kotlin.** Плагин `org.jetbrains.kotlin.android` к модулям не применяется
  (AGP 9 падает с ошибкой). В корневом `build.gradle.kts` он объявлен с `apply false` только чтобы
  зафиксировать версию KGP на classpath — она должна совпадать с версией Compose compiler plugin. См. D-011.
- `jvmTarget` не задаём: с built-in Kotlin он равен `compileOptions.targetCompatibility` (17).
- Хэш коммита и номер сборки попадают в `BuildConfig.GIT_SHA` / `BuildConfig.BUILD_NUMBER`:
  в CI из `GITHUB_SHA` / `GITHUB_RUN_NUMBER`, локально из `git rev-parse` / `"local"`.
- Экран показывает также версию Android, API level и модель устройства — это ответ на открытый
  вопрос 1 из `STATUS.md`, его можно прочитать прямо с телефона.
- Иконка: адаптивная, полностью векторная (`res/drawable/ic_launcher_{background,foreground,monochrome}.xml`
  + `res/mipmap-anydpi/ic_launcher.xml`), сгенерирована из `assets/icon/syto-icon.svg` скриптом
  `scripts/svg2vector.py` (175 точек → 21 path, все в safe zone 66 dp). PNG-mipmap не нужны при minSdk 29.
  Если SVG меняется — перезапустить скрипт, руками XML не править.

### 0.2 — CI: сборка APK

Статус: ✅ (2026-09-24, run #1 зелёный, артефакт на месте)

**Цель.** На каждый пуш в любую ветку GitHub Actions собирает `app-debug.apk` и выкладывает артефактом.

**Делает агент.**

- `.github/workflows/build.yml`: checkout, JDK 17, Android SDK (action), gradle cache, `assembleDebug`, `upload-artifact` с APK. Имя артефакта включает короткий хэш.
- Debug-подпись — стандартный debug keystore. Стабильная подпись важна, иначе телефон
  не даст обновить приложение поверх старой версии: keystore либо генерируется детерминированно, либо хранится в secrets. Решение записать в `DECISIONS.md`.

**Проверяет пользователь.**

- [ ] Открыть workflow run на GitHub → скачать артефакт → в архиве есть APK. Результат: …

**DoD.** Зелёный workflow с артефактом.

**Результат.**

- 2026-09-24. Workflow `Build debug APK` (`.github/workflows/build.yml`): триггер — любой пуш в любую
  ветку + ручной запуск; `actions/checkout@v7` → `actions/setup-java@v6` (Temurin 17) →
  `gradle/actions/setup-gradle@v6` (кэш + проверка wrapper) → `./gradlew assembleDebug` →
  артефакт `syto-debug-<sha7>` с файлом `syto-debug-<sha7>.apk`, хранится 30 дней.
  Отдельный action для Android SDK не нужен: на `ubuntu-latest` SDK уже стоит (платформы 34…37,
  Build-Tools 36/37, лицензии приняты; недостающее AGP докачает сам).
- Подпись: коммитим `app/debug.keystore` (D-009). Детерминированно сгенерировать keystore нельзя
  (ключ случайный), secrets усложняют локальную сборку и форки. Debug-ключ секретом не является.
- Первый прогон — [run #1](https://github.com/merenkoff/syto/actions/runs/35968594731), коммит `72ee01a`:
  зелёный с первой попытки, job `assembleDebug` 2 мин 8 с (сам `./gradlew assembleDebug` — 1 мин 56 с на холодном кэше).
  Артефакт `syto-debug-72ee01a`, zip 10,4 МБ, внутри `syto-debug-72ee01a.apk`. Хранится до 2026-10-24.

### 0.3 — Установка на телефон

Статус: 🔄 (инструкция написана, чек-лист за пользователем)

**Цель.** Понятный, описанный путь «скачать → установить → обновить поверх».

**Делает агент.**

- Раздел в `docs/phases/phase-0-scaffold.md` (здесь, ниже, «Как ставить APK») со скриншот-независимой инструкцией.
- Если ставить артефакты неудобно (zip, логин в GitHub с телефона), запасной вариант: workflow публикует pre-release `nightly` с APK, который умеет тянуть Obtainium. Решаем по факту.

**Проверяет пользователь.**

- [ ] Установить APK, запустить, увидеть версию и хэш. Результат: …
- [ ] Запушить любое изменение текста, дождаться CI, поставить новый APK поверх без удаления старого. Результат: …
- [ ] Переписать с экрана строки «Android» и «Device» в `DECISIONS.md` (гейт G0). Результат: …

**DoD.** Все пункты чек-листа ✅. Модель и Android-версия устройства записаны в `DECISIONS.md`.

**Результат.** —

## Как ставить APK

Три пути. Основной по D-008 — первый.

### A. С Mac через adb (основной)

1. Открыть репозиторий на GitHub → вкладка **Actions** → workflow **Build debug APK** →
   нужный прогон (по сообщению коммита или хэшу) → внизу блок **Artifacts** → скачать `syto-debug-<sha7>`.
   GitHub отдаёт zip; внутри один файл `syto-debug-<sha7>.apk`.
2. Телефон подключён по USB, отладка по USB включена, `adb devices` показывает устройство.
3. ```bash
   unzip -o ~/Downloads/syto-debug-<sha7>.zip -d ~/Downloads/syto
   adb install -r ~/Downloads/syto/syto-debug-<sha7>.apk
   ```
   `-r` ставит поверх уже установленной версии, данные приложения сохраняются.
4. Открыть Syto. На экране: версия, `Commit` = тот же `<sha7>`, `Build` = номер прогона CI,
   строки `Android` и `Device`.

Локальная сборка на Mac делает то же самое без GitHub:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Локальный APK подписан тем же `app/debug.keystore`, поэтому CI-сборка и локальная сборка ставятся
поверх друг друга в любом порядке. На экране у локальной сборки `Build` = `local`.

### B. Только с телефона

1. В браузере телефона зайти на GitHub (нужен логин — артефакты без него не скачиваются) →
   Actions → прогон → Artifacts → скачать zip.
2. Открыть zip в приложении «Файлы» (или любом файловом менеджере), извлечь APK, нажать на него.
3. При первом разе Android попросит разрешить установку из этого источника
   («Установка неизвестных приложений» для Файлов/браузера). Разрешить.
4. Дальше как в A, шаг 4.

Если этот путь окажется слишком неудобным (zip, логин), включаем путь C.

### C. Запасной: pre-release `nightly` + Obtainium (не реализован)

Workflow дополнительно публикует pre-release с тегом `nightly` и прикреплённым APK; на телефоне
Obtainium подписывается на репозиторий и ставит обновления сам. Требует `contents: write` в workflow.
Делаем только если B неудобен — решение записать сюда и в `DECISIONS.md`.

### Если телефон не даёт поставить поверх

- «Приложение не установлено» / `INSTALL_FAILED_UPDATE_INCOMPATIBLE` — старая сборка подписана
  другим ключом (например, поставлена из Android Studio до D-009). Один раз удалить Syto и поставить заново.
- `INSTALL_FAILED_VERSION_DOWNGRADE` — не должно случаться, пока `versionCode = 1` (D-010).
  Если случилось — `adb install -r -d …` для debug-сборок разрешает даунгрейд.

## Заметки и находки

- Облачная сессия не может ни собрать, ни сконфигурировать проект: `dl.google.com` закрыт прокси.
  Любая правка `build.gradle.kts` / `libs.versions.toml` из облака проверяется только пушем и CI.
- Отсюда правило: из облака — маленькие пуши, смотреть прогон, чинить. Не копить непроверенные изменения.
- CI run #1 (2026-09-24): зелёный, ~2 мин на холодном кэше. Ни одна из версий из таблицы 0.1 не потребовала правок.
- Проверено локально на чистом Gradle 9.6.1 (без Android-плагина): аксессоры каталога и провайдер хэша.
  Нюанс каталога: алиас `androidx-compose-ui` одновременно лист и префикс для `androidx-compose-ui-tooling*`,
  поэтому `libs.androidx.compose.ui` — не `Provider`, а объект-группа. В `implementation(...)` его передавать можно,
  `.get()` на нём не работает — нужен `.asProvider()`.
