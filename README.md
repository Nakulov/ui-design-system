# ui-design-system
Platform-independent UI design system contracts, schemas, and AI-assisted component generation

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
python3 tools/validate.py --base origin/dev
```

Для работы с ассистентом в `.claude/skills/` есть скиллы: `contract-authoring` (проектирование контрактов), `groom`
(«давай погрумим …») и `take` («берём в работу #N»).
