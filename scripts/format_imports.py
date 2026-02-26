import os
import re


def process_file(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    lines = content.split("\n")

    package_idx = -1
    import_lines = []

    for i, line in enumerate(lines):
        if line.startswith("package "):
            package_idx = i
        elif line.startswith("import "):
            import_lines.append(line)

    if not import_lines or package_idx == -1:
        return

    # To check usage, we take everything after the package declaration, excluding the imports themselves
    # And we remove comments to avoid keeping imports only used in comments (optional, but let's just keep it simple and safe: we search the raw body, maybe keeping imports used in comments is harmless).
    # Wait, the user specifically saw a warning about unused lombok import. If it's used in comments it might still warn! So we should remove comments?
    # Actually, javadoc might use {@link User}. We should keep it.

    code_body_lines = []
    for i in range(package_idx + 1, len(lines)):
        if not lines[i].startswith("import "):
            code_body_lines.append(lines[i])

    code_body = "\n".join(code_body_lines)

    used_imports = []

    for imp in import_lines:
        imp_clean = imp.strip()
        if not imp_clean:
            continue

        is_wildcard = imp_clean.endswith("*;")
        if is_wildcard:
            used_imports.append(imp_clean)
            continue

        match = re.search(r"([A-Za-z0-9_]+);$", imp_clean)
        if match:
            class_name = match.group(1)
            # Regex to find whole word usage
            if re.search(r"\b" + class_name + r"\b", code_body):
                used_imports.append(imp_clean)
            else:
                pass  # removed
        else:
            used_imports.append(imp_clean)

    used_imports = list(set(used_imports))

    # Sort
    def sort_key(imp):
        imp_body = (
            imp.replace("import ", "").replace("static ", "").replace(";", "").strip()
        )
        prefix = 4
        if imp_body.startswith("java."):
            prefix = 0
        elif imp_body.startswith("javax."):
            prefix = 1
        elif imp_body.startswith("org."):
            prefix = 2
        elif imp_body.startswith("com."):
            prefix = 3

        is_static = 1 if "static " in imp else 0

        return (is_static, prefix, imp_body)

    used_imports.sort(key=sort_key)

    # Reconstruct
    out_lines = lines[: package_idx + 1]
    out_lines.append("")

    current_prefix = None
    for imp in used_imports:
        prefix = sort_key(imp)[1]
        is_static = sort_key(imp)[0]

        # group by prefix and static/non-static
        group_key = (is_static, prefix)
        if current_prefix is not None and current_prefix != group_key:
            out_lines.append("")
        current_prefix = group_key

        out_lines.append(imp)

    out_lines.append("")

    # Trim leading empty lines from code_body_lines
    while code_body_lines and not code_body_lines[0].strip():
        code_body_lines.pop(0)

    out_lines.extend(code_body_lines)

    new_content = "\n".join(out_lines)

    if new_content != content:
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Formatted: {filepath}")


def main():
    path = "/Users/yingjun.bian/Code/landfunBoot/src/main/java"
    for root, dirs, files in os.walk(path):
        for file in files:
            if file.endswith(".java"):
                process_file(os.path.join(root, file))


if __name__ == "__main__":
    main()
