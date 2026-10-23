---
name: groom
description: Груминг задачи дизайн-системы — обсуждение с пользователем и оформление результата задачей в GitHub на доске проекта. Использовать, когда пользователь говорит «давай погрумим …», «обсудим задачу …», «заведи задачу …».
---

# Груминг задачи

## Назначение

Превращает тему (компонент, foundation-сущность, изменение спецификации, процесс) в задачу GitHub, готовую к работе: с
согласованным решением, открытыми вопросами и критериями готовности. Файлы репозитория при груминге не меняются.

## Ход работы

1. **Контекст.** Проверь, нет ли уже задачи по теме: `gh issue list --state open --search "<тема>"`. Если есть —
   продолжай её, а не заводи новую. Прочитай связанные спецификации и контракты.
2. **Обсуждение.** Для контрактов следуй скиллу `contract-authoring`: потребность и поведение, пример в месте
   использования, альтернативы с рекомендацией, по одному решению за раз. Различай согласовано, предложено, не
   определено.
3. **Размер.** Если тема не помещается в один PR, предложи эпик (метка `epic`) с подзадачами; каждая подзадача —
   отдельный груминг или пункт эпика в Backlog.
4. **Черновик.** Покажи пользователю задачу по шаблону `.github/ISSUE_TEMPLATE/task.md`: заголовок, метки, все разделы.
   В «Результатах обсуждения» запиши и отклонённые варианты с причиной. Критерии готовности — проверяемые пункты,
   первый — `python3 tools/validate.py` проходит.
5. **Создание** — только после одобрения черновика:
   - `gh issue create --title … --label … --body-file …`;
   - добавь на доску и поставь статус Groomed функцией `board_status` (раздел «Доска»);
   - подзадачу эпика привяжи: `gh api repos/{owner}/{repo}/issues/<эпик>/sub_issues -F sub_issue_id=<id задачи>`.
6. Сообщи номер и ссылку на задачу.

Темы, которые всплыли по ходу и не входят в задачу, предложи завести отдельно в Backlog с кратким описанием.

## Доска

Проект пользователя `Nakulov` номер 8 «UI Design System», id `PVT_kwHOASpeKc4BoUwf`. Поле Status
`PVTSSF_lAHOASpeKc4BoUwfzhm1ZcE`:

| Статус      | id         |
|-------------|------------|
| Backlog     | `ddf5cd1d` |
| Groomed     | `ae278317` |
| In progress | `1da44a5c` |
| In review   | `95554372` |
| Done        | `7f87e422` |

Статус ставь только функцией `board_status`: она добавляет задачу на доску, ставит статус и проверяет, что задача видна
в списке элементов доски.

Добавление через GraphQL (`addProjectV2ItemById`) ненадёжно: элемент может навсегда остаться невидимым в списке доски
(GraphQL `items`, `gh project item-list`, веб-интерфейс), хотя задача считает себя на доске
(`gh issue view N --json projectItems`); пересоздание через GraphQL помогает не всегда (#10). Поэтому функция добавляет
задачу через REST API доски. Если задача уже на доске, REST отвечает `422 Content already exists`, и функция берёт
существующий элемент из `projectItems` задачи. Видимость проверяется и в REST-, и в GraphQL-списке несколько раз; если
задачи нет — элемент удаляется и добавляется заново. Оболочка — zsh: не разбивай строки через `set -- $var`, передавай
номер задачи и статус отдельными аргументами.

```sh
board_status() {  # board_status <номер задачи> <id статуса>
  local p=PVT_kwHOASpeKc4BoUwf f=PVTSSF_lAHOASpeKc4BoUwfzhm1ZcE items=users/Nakulov/projectsV2/8/items rid node attempt check
  rid=$(gh api "repos/Nakulov/ui-design-system/issues/$1" --jq .id) || return 1
  for attempt in 1 2; do
    node=$(gh api -X POST $items -f type=Issue -F id="$rid" --jq .node_id 2>/dev/null) ||
      node=$(gh api graphql -F n="$1" -f query='query($n:Int!){repository(owner:"Nakulov",name:"ui-design-system"){issue(number:$n){projectItems(first:20){nodes{id project{number}}}}}}' \
        --jq '.data.repository.issue.projectItems.nodes[] | select(.project.number == 8) | .id')
    gh project item-edit --id "$node" --project-id $p --field-id $f --single-select-option-id "$2" >/dev/null || return 1
    for check in 1 2 3 4 5; do
      gh api "$items?per_page=100" --jq '.[].node_id' | grep -qx "$node" &&
        gh api graphql -f query='{user(login:"Nakulov"){projectV2(number:8){items(first:100){nodes{id}}}}}' \
          --jq '.data.user.projectV2.items.nodes[].id' | grep -qx "$node" && return 0
    done
    [ $attempt = 1 ] && gh api graphql -f query='mutation($p:ID!,$i:ID!){deleteProjectV2Item(input:{projectId:$p,itemId:$i}){deletedItemId}}' \
      -f p=$p -f i="$node" >/dev/null
  done
  echo "#$1 не видна на доске" >&2
  return 1
}

board_status 7 ae278317   # задача #7 → Groomed
```

Если функция вернула ошибку, сообщи пользователю: задача создана, но на доске не видна.

Список читает первые 100 элементов; когда задач станет больше, проверку нужно перевести на постраничное чтение.

Метки: `component`, `foundation`, `spec`, `process`, `epic`.

## Границы

- Не создавай задачу без одобрения черновика и не меняй файлы репозитория.
- Не выдавай предложенное за согласованное: нерешённое остаётся в «Открытых вопросах».
