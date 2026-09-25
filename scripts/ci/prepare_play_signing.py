"""Restore signing inputs only on an ephemeral GitHub-hosted release runner."""

import base64
import os
from pathlib import Path
import shutil
import sys


def property_value(value):
    """Escape Java Properties values, including whitespace and non-ASCII passwords."""
    escapes = {"\\": "\\\\", "\n": "\\n", "\r": "\\r", "\t": "\\t", " ": "\\ ", "=": "\\=", ":": "\\:"}
    result = []
    for char in value:
        if char in escapes:
            result.append(escapes[char])
        elif ord(char) < 32 or ord(char) > 126:
            encoded = char.encode("utf-16-be")
            result.extend("\\u" + encoded[i:i + 2].hex() for i in range(0, len(encoded), 2))
        else:
            result.append(char)
    return "".join(result)


def main():
    if os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("RUNNER_ENVIRONMENT") != "github-hosted":
        raise ValueError("Signing preparation is only supported on a GitHub-hosted Actions runner.")
    names = ["ANDROID_UPLOAD_KEYSTORE_BASE64", "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD", "ANDROID_STORE_PASSWORD", "PROD_PROPERTIES"]
    for name in names:
        if not os.environ.get(name, "").strip():
            raise ValueError("Missing GitHub Environment secret: " + name)
    os.umask(0o077)
    signing_file = Path(os.environ["RUNNER_TEMP"]) / "gayadi-upload.jks"
    try:
        decoded = base64.b64decode("".join(os.environ["ANDROID_UPLOAD_KEYSTORE_BASE64"].split()), validate=True)
    except ValueError:
        raise ValueError("ANDROID_UPLOAD_KEYSTORE_BASE64 is not valid Base64.") from None
    if not decoded:
        raise ValueError("Upload keystore is empty.")
    signing_file.write_bytes(decoded)
    values = {
        "storeFile": str(signing_file),
        "keyAlias": os.environ["ANDROID_KEY_ALIAS"],
        "keyPassword": os.environ["ANDROID_KEY_PASSWORD"],
        "storePassword": os.environ["ANDROID_STORE_PASSWORD"],
    }
    Path("keystore.properties").write_text(
        "".join(key + "=" + property_value(value) + "\n" for key, value in values.items()), encoding="ascii",
    )
    Path("config/prod.properties").write_text(os.environ["PROD_PROPERTIES"], encoding="utf-8")
    shutil.copyfile("config/dev.properties.example", "config/dev.properties")


if __name__ == "__main__":
    try:
        main()
    except ValueError as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
    except Exception:
        print("Could not prepare release configuration.", file=sys.stderr)
        sys.exit(1)
