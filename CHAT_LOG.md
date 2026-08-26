# Session Log & Chat History Record

## [2026-08-24 09:15 UTC] - Full Project Recreation & Sync Hardening
- Полное пересоздание всех файлов Android, манифестов и конфигураций.
- **Версия**: 1.0.4 (VersionCode 9).
- Проверены Burst Mode, фантомные метки `Ф1, Ф2`, OCR Latin, экспорт/импорт профилей и CI/CD workflow.

## [2026-08-26 20:07 UTC] - GitHub Sync Verified
- Пользователь подтвердил успешную доставку версии 1.0.4 (Build 9) в репозиторий GitHub.
- Все модули (Burst Mode, OCR Latin, макросы, фантомные метки и экспорт/импорт профилей) синхронизированы.

## [2026-08-26 20:11 UTC] - Fix AndroidManifest Merge Conflict (allowBackup)
- **Проблема**: Сборка на GitHub Actions падала на шаге `:app:processReleaseMainManifest` из-за конфликта `allowBackup=true` нашего манифеста и `allowBackup=false` внутри библиотеки `ml-computer-vision-ocr-latin-model`.
- **Решение**: Добавлены `xmlns:tools="http://schemas.android.com/tools"` и `tools:replace="android:allowBackup"` в `<application>` внутри `app/src/main/AndroidManifest.xml`.
- **Версия**: обновлена до 1.0.5 (VersionCode 10).


