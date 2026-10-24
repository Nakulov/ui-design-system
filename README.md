# ui-design-system
Platform-independent UI design system contracts, schemas, and AI-assisted component generation

Видение, границы, текущее состояние и этапы развития — в [ROADMAP.md](ROADMAP.md).

## Процесс

Задачи ведутся в [GitHub Issues](https://github.com/Nakulov/ui-design-system/issues) и на доске
[UI Design System](https://github.com/users/Nakulov/projects/8): Backlog → Groomed → In progress → In review → Done.

1. **Груминг.** Тема обсуждается и оформляется задачей по шаблону `.github/ISSUE_TEMPLATE/task.md`: проблема,
   решение, открытые вопросы, результаты обсуждения, объём изменений, критерии готовности. Крупные темы — эпики
   (метка `epic`) с подзадачами.
2. **Работа.** На задачу — ветка `task/<N>-<slug>` от `dev`. Решения, принятые по ходу, записываются комментариями в
   задачу.
3. **PR в `dev`** по шаблону `.github/pull_request_template.md` с `Closes #N`. Ветка `dev` защищена: изменения только
   через PR с успешной проверкой, слияние — squash.

Проверка контрактов, файлов значений и версий спецификаций запускается в PR и локально:

```sh
python3 -m pip install -r tools/requirements.txt   # один раз
python3 tools/validate.py --base origin/dev
```

Примеры моделей лежат в `samples/` каталогов сущностей: `min.json` и `max.json` генерируются из контракта,
именованные примеры пишутся вручную. После изменения контрактов или примеров обновите производные файлы — примеры
`min` и `max`, блоки примеров в README и список контрактов для подсказок IDE:

```sh
python3 tools/sync.py
```

Модель, например полученную с сервера, можно проверить по контрактам:

```sh
python3 tools/validate_model.py model.json
```

Структура контрактов и файлов значений описана мета-схемами `specifications/contract.schema.json` и
`specifications/values.schema.json`; в Android Studio и других IDE JetBrains они подключены для подсказок через
`.idea/jsonSchemas.xml`. Контракты перечислены в нём поимённо (маски IntelliJ не умеют исключать `samples/`), список
генерирует `tools/sync.py`.

Для работы с ассистентом в `.claude/skills/` есть скиллы: `contract-authoring` (проектирование контрактов), `groom`
(«давай погрумим …») и `take` («берём в работу #N»).
