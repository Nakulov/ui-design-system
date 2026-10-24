# Плагин Figma

Строит в Figma библиотеку и документацию дизайн-системы из набора данных (`specifications/bundle-format.md`). Задачи
плагина — эпик этапа 1 в `ROADMAP.md`. Сейчас плагин открывает набор, проверяет версию формата и показывает сводку;
по кнопке «Построить foundation» создаёт или обновляет на месте коллекции переменных, текстовые стили, стили эффектов и
компоненты символов (`figma-mapping.md`, раздел 3.1) и показывает отчёт.

## Сборка

Нужен Node из `.nvmrc`.

```sh
python3 tools/bundle.py          # из корня репозитория: набор в build/bundle.json
cd figma-plugin
npm ci
npm run build                    # dist/code.js и dist/ui.html
npm run typecheck
```

`npm run watch` пересобирает плагин при изменении исходников.

## Запуск в Figma

1. В Figma Desktop: **Plugins → Development → Import plugin from manifest…** и выберите `figma-plugin/manifest.json`.
2. Запустите плагин **UI Design System**, откройте `build/bundle.json` и нажмите «Построить foundation».
   Коллекциям с двумя режимами (`light`, `dark`) нужен платный тариф Figma.

## Устройство

| Файл                | Роль                                                                    |
|---------------------|-------------------------------------------------------------------------|
| `src/code.ts`       | Основной поток: доступ к документу Figma, хранит открытый набор.        |
| `src/ui.ts`         | Окно плагина: выбор файла набора и сводка.                              |
| `src/ui.html`       | Разметка окна; сборка встраивает в неё скрипт окна.                     |
| `src/bundle.ts`     | Типы набора по `bundle-format.md` и проверка поддерживаемой версии.     |
| `src/messages.ts`   | Сообщения между окном и основным потоком.                               |
| `src/foundation.ts` | Построение foundation: переменные, стили, символы, удаление устаревших. |
| `src/report.ts`     | Отчёт: счётчики по видам объектов и предупреждения.                     |
| `build.mjs`         | Сборка esbuild.                                                         |

Основной поток и окно проверяются разными конфигурациями TypeScript (`tsconfig.json` и `tsconfig.ui.json`): типы
Figma и DOM несовместимы в одной программе. Типы набора пишутся вручную и меняются вместе с `bundle-format.md`;
поддерживаемая версия — `SUPPORTED_FORMAT` в `src/bundle.ts`.
