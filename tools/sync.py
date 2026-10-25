#!/usr/bin/env python3
"""Обновление производных файлов: примеры min и max, блоки примеров в README, сопоставление схем в IDE.

Запуск: python3 tools/sync.py
Запускается после изменения контрактов или примеров; валидатор проверяет, что всё актуально
(schema-format.md, разделы 11.1, 11.2, 12).
"""

import sys

import idea
from contracts import CONTRACTS, ROOT, load_registry
from samples import dumps, generate, render, required_samples


def write(path, text):
    if not path.is_file() or path.read_text() != text:
        path.parent.mkdir(exist_ok=True)
        path.write_text(text)
        print(f"обновлён {path.relative_to(ROOT)}")


def main():
    registry = load_registry()
    for contract in registry:
        for mode in required_samples(contract):
            write(contract.path.parent / "samples" / f"{mode}.json", dumps(generate(registry, contract, mode)))
    write(idea.PATH, idea.json_schemas(registry))

    failed = False
    for readme in sorted(CONTRACTS.glob("*/*/README.md")):
        text, problems = render(readme)
        for problem in problems:
            print(f"{readme.relative_to(ROOT)}: {problem}")
        failed |= bool(problems)
        if not problems:
            write(readme, text)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
