#!/usr/bin/env bash
# Доска проекта: статусы задач, создание задач, подзадачи эпика.
#
#   tools/board.sh status <задача> <статус>         статус: backlog, groomed, in-progress, in-review, done
#   tools/board.sh issue <заголовок> <метка> <файл>  создаёт задачу, печатает номер
#   tools/board.sh sub <эпик> <задача>              привязывает задачу к эпику
#
# Пример: n=$(tools/board.sh issue "Компонент Image" component body.md) &&
#   tools/board.sh status "$n" groomed && tools/board.sh sub 3 "$n"
set -u

repo=Nakulov/ui-design-system
project=PVT_kwHOASpeKc4BoUwf
field=PVTSSF_lAHOASpeKc4BoUwfzhm1ZcE
items=users/Nakulov/projectsV2/8/items

status_id() {
  case "$(echo "$1" | tr 'A-Z_ ' 'a-z--')" in
    backlog) echo ddf5cd1d ;;
    groomed) echo ae278317 ;;
    in-progress) echo 1da44a5c ;;
    in-review) echo 95554372 ;;
    done) echo 7f87e422 ;;
    *) echo "неизвестный статус: $1 (backlog, groomed, in-progress, in-review, done)" >&2; return 1 ;;
  esac
}

# Добавление через GraphQL (addProjectV2ItemById) ненадёжно: элемент может остаться невидимым в списке доски (#10).
# Поэтому задача добавляется через REST; если она уже на доске (422), элемент берётся из projectItems задачи.
# Видимость проверяется в REST- и GraphQL-списке; если задачи нет — элемент удаляется и добавляется заново.
# Списки читаются по первым 100 элементам.
status() {
  local opt rid node attempt check
  opt=$(status_id "$2") || return 1
  rid=$(gh api "repos/$repo/issues/$1" --jq .id) || return 1
  for attempt in 1 2; do
    node=$(gh api -X POST $items -f type=Issue -F id="$rid" --jq .node_id 2>/dev/null) ||
      node=$(gh api graphql -F n="$1" -f query='query($n:Int!){repository(owner:"Nakulov",name:"ui-design-system"){issue(number:$n){projectItems(first:20){nodes{id project{number}}}}}}' \
        --jq '.data.repository.issue.projectItems.nodes[] | select(.project.number == 8) | .id')
    [ -n "$node" ] || { echo "#$1: элемент доски не найден" >&2; return 1; }
    gh project item-edit --id "$node" --project-id $project --field-id $field --single-select-option-id "$opt" >/dev/null || return 1
    for check in 1 2 3 4 5; do
      gh api "$items?per_page=100" --jq '.[].node_id' | grep -qx "$node" &&
        gh api graphql -f query='{user(login:"Nakulov"){projectV2(number:8){items(first:100){nodes{id}}}}}' \
          --jq '.data.user.projectV2.items.nodes[].id' | grep -qx "$node" && return 0
    done
    [ $attempt = 1 ] && gh api graphql -f query='mutation($p:ID!,$i:ID!){deleteProjectV2Item(input:{projectId:$p,itemId:$i}){deletedItemId}}' \
      -f p=$project -f i="$node" >/dev/null
  done
  echo "#$1 не видна на доске" >&2
  return 1
}

issue() {
  local url
  url=$(gh issue create --title "$1" --label "$2" --body-file "$3") || return 1
  case "${url##*/}" in (''|*[!0-9]*) echo "задача не создана: $url" >&2; return 1;; esac
  echo "${url##*/}"
}

sub() {
  local id
  id=$(gh api "repos/$repo/issues/$2" --jq .id) || return 1
  gh api "repos/$repo/issues/$1/sub_issues" -F sub_issue_id="$id" >/dev/null
}

case "${1:-}" in
  status|sub) [ $# = 3 ] || { echo "нужно: $1 <номер> <значение>" >&2; exit 2; }; "$@" ;;
  issue) [ $# = 4 ] || { echo "нужно: issue <заголовок> <метка> <файл тела>" >&2; exit 2; }; "$@" ;;
  *) sed -n '2,9p' "$0" >&2; exit 2 ;;
esac
