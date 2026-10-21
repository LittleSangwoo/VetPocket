# VetPocket — Android-приложение для ухода за питомцем

Kotlin + Jetpack Compose. Реализовано по ТЗ «Техническая документация проекта VetPocket».

## Как запустить (Android Studio Panda 2 | 2025.3.2)

1. `File → Open` → выберите папку `VetPocket`. Дождитесь Gradle Sync (JDK 21 из Android Studio подходит).
   Если студия предложит «Upgrade AGP» — можно отказаться: проект рассчитан на AGP 8.13.0 / Gradle 8.13.
2. Откройте `local.properties` (его создаёт студия, там уже есть `sdk.dir`) и допишите ключи — шаблон в `local.properties.example`:
   ```
   YANDEX_MAPKIT_KEY=...      # developer.tech.yandex.ru → MapKit Mobile SDK
   LLM_PROVIDER=YANDEX_GPT    # или GROQ
   LLM_API_KEY=...
   LLM_FOLDER_ID=...          # только для YANDEX_GPT
   ```
3. `Sync Project`, затем Run на устройстве/эмуляторе (minSdk 26).
   Без ключей приложение запускается: экраны «Главная» и «Медкарта» работают полностью,
   на «Карте» и в анализаторе корма появится подсказка, какого ключа не хватает.

## Соответствие ТЗ

| Требование ТЗ | Где реализовано |
|---|---|
| Compose, ViewModel + StateFlow | `ui/*/…ViewModel.kt`, `ui/*/…Screen.kt` |
| Room: PetProfile, WeightRecord, MedicalEvent, DiaryNote | `data/db/` |
| Retrofit2 + kotlinx.serialization, YandexGPT / Groq (GigaChat — не реализован) | `data/remote/` |
| AlarmManager: напоминания о прививках (+ восстановление после перезагрузки) | `reminders/` |
| CameraX + ML Kit (локально) | `ui/scanner/CameraCapture.kt`, `ScannerViewModel.kt` |
| Yandex MapKit: клиники, фильтр «Открыто прямо сейчас», маршрут, звонок | `ui/map/` |
| Vico: график веса | `ui/medcard/MedCardScreen.kt` |
| System Prompt из профиля Room | `PromptBuilder` в `data/remote/LlmRepository.kt` |

## Важно знать

* **Кириллица и ML Kit.** Офлайн-распознавание ML Kit официально не поддерживает кириллицу, поэтому на русских
  упаковках текст может распознаваться плохо. Поэтому шаг «ручное подтверждение» — это редактируемое поле:
  исправьте текст перед отправкой. Для русскоязычных составов позже можно заменить распознаватель.
* **Groq** может быть недоступен из РФ — в этом случае используйте `LLM_PROVIDER=YANDEX_GPT`.
* **MapKit `-full`** нужен именно для поиска организаций (в `-lite` поиска нет).
* **Vico зафиксирован на 2.1.3**: в ветке 3.x API другой.
* Версии библиотек — в `gradle/libs.versions.toml`. Если Sync ругается на конкретную версию, поменяйте её там.
* Оценка корма от нейросети — справочная, не замена ветеринару (дисклеймер есть в интерфейсе).
