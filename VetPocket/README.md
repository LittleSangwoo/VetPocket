# VetPocket — Android-приложение для ухода за питомцем

Kotlin + Jetpack Compose. Реализовано по ТЗ «Техническая документация проекта VetPocket».

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
* Оценка корма от нейросети — справочная, не замена ветеринару (дисклеймер есть в интерфейсе).
