"""Validate text-conversation JSONL before adapting it to a training provider."""

import argparse
import json
import sys


def validate(path):
    errors = []
    count = 0
    seen = set()
    with open(path, encoding="utf-8") as source:
        for line_number, line in enumerate(source, 1):
            try:
                record = json.loads(line)
                messages = record.get("messages") if isinstance(record, dict) else None
                if not isinstance(messages, list) or len(messages) < 2:
                    raise ValueError("messages must contain at least two turns")
                for message in messages:
                    if not isinstance(message, dict):
                        raise ValueError("each message must be an object")
                    if message.get("role") not in {"system", "user", "assistant"}:
                        raise ValueError("text-only validator supports system/user/assistant roles")
                    content = message.get("content")
                    if not isinstance(content, str) or not content.strip():
                        raise ValueError("message content must be non-empty text")
                if not any(m["role"] == "user" for m in messages):
                    raise ValueError("missing user turn")
                if messages[-1]["role"] != "assistant":
                    raise ValueError("last turn must be an assistant target")
                key = json.dumps(messages, sort_keys=True, ensure_ascii=False)
                if key in seen:
                    raise ValueError("duplicate conversation")
                seen.add(key)
                count += 1
            except (ValueError, TypeError) as error:
                errors.append(f"line {line_number}: {error}")
    if not count:
        errors.append("dataset contains no valid conversations")
    return count, errors


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("dataset")
    args = parser.parse_args()
    try:
        count, errors = validate(args.dataset)
    except OSError as error:
        parser.exit(1, f"{error}\n")
    for error in errors:
        print(error, file=sys.stderr)
    print(f"{count} valid conversations; {len(errors)} errors")
    sys.exit(bool(errors))
