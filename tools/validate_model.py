#!/usr/bin/env python3
"""Проверка модели по контрактам (schema-format.md, раздел 4.5).

Запуск: python3 tools/validate_model.py <файл> [--contract <идентификатор контракта>]
По умолчанию корень модели — компонент: объект с одним ключом — именем компонента.
Идентификатор контракта — <слой>/<сущность>/<Имя>, например core/layout/Layout.
Выход 1, если найдены ошибки.
"""

import argparse
import json
import sys

from contracts import COMPONENT, load_registry


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("file", help="JSON-файл модели")
    parser.add_argument("--contract", default=COMPONENT, help=f"контракт корня модели (по умолчанию {COMPONENT})")
    args = parser.parse_args()

    registry = load_registry()
    contract = registry.by_id.get(args.contract)
    if contract is None:
        sys.exit(f"нет контракта {args.contract}")
    try:
        with open(args.file) as f:
            value = json.load(f)
    except (OSError, json.JSONDecodeError) as e:
        sys.exit(f"{args.file}: {e}")

    problems = list(registry.value_errors(value, contract, "модель"))
    for problem in problems:
        print(problem)
    print(f"Ошибок: {len(problems)}" if problems else "Ошибок нет")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
