"""Allocate stable release IDs and check Play before uploading. No third-party deps."""

import json
import os
from pathlib import Path
import re
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

PACKAGE_NAME = "com.doonow.gayadi"
MAX_VERSION_CODE = 2_100_000_000


def release_target(ref):
    targets = {
        "refs/heads/stg": {"track": "internal", "environment": "play-internal"},
        "refs/heads/main": {"track": "production", "environment": "play-production"},
    }
    if ref not in targets:
        raise ValueError("Play releases are only allowed from main or stg.")
    return targets[ref]


def release_metadata(base, run_number, sha):
    if not re.fullmatch(r"[0-9]+", base or ""):
        raise ValueError("Configure PLAY_VERSION_CODE_BASE using the highest previously used Play version code.")
    if not re.fullmatch(r"[1-9][0-9]*", run_number or ""):
        raise ValueError("GITHUB_RUN_NUMBER must be a positive integer.")
    if not re.fullmatch(r"[0-9a-f]{40}", sha or ""):
        raise ValueError("GITHUB_SHA must be a full commit SHA.")
    code = int(base) + int(run_number)
    if not 1 <= code <= MAX_VERSION_CODE:
        raise ValueError("Release version code is outside the Google Play range.")
    return {"version_code": str(code), "release_name": "ci-{}-{}".format(code, sha)}


def already_uploaded(tracks, bundles, code, name, target_track):
    """Only an exact, completed release on the selected track can be skipped."""
    if target_track not in {"internal", "production"}:
        raise ValueError("Unsupported Play track.")
    observed_codes = {int(bundle["versionCode"]) for bundle in bundles}
    exact_match = False
    for track in tracks:
        for release in track.get("releases", []):
            codes = {int(value) for value in release.get("versionCodes", [])}
            observed_codes.update(codes)
            if code not in codes:
                continue
            if release.get("name") != name:
                raise ValueError("Version code belongs to a different release; check PLAY_VERSION_CODE_BASE.")
            if track.get("track") == target_track and release.get("status") == "completed":
                exact_match = True
    if exact_match:
        return True
    if code in observed_codes:
        raise ValueError("Version already exists but is not a completed matching release on the selected track. Resolve it in Play Console; do not reupload.")
    if observed_codes and code <= max(observed_codes):
        raise ValueError("A newer version is already in Play. Start a new release run on the intended branch or correct PLAY_VERSION_CODE_BASE.")
    return False


class PlayClient:
    def __init__(self, token):
        if not token:
            raise ValueError("Play access token is missing.")
        self.token = token
        self.base_url = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/" + PACKAGE_NAME

    def request(self, method, path, body=None):
        request = Request(
            self.base_url + path,
            data=json.dumps(body).encode("utf-8") if body is not None else None,
            headers={"Authorization": "Bearer " + self.token, "Content-Type": "application/json"},
            method=method,
        )
        try:
            with urlopen(request, timeout=60) as response:
                payload = response.read()
                return json.loads(payload) if payload else {}
        except HTTPError as error:
            # Do not include response bodies, tokens or request headers in CI logs.
            raise RuntimeError("Play API {} failed (HTTP {}). Check API access and app permissions.".format(method, error.code)) from None
        except (URLError, TimeoutError):
            raise RuntimeError("Play API connection failed; retry the workflow.") from None


def check_play(client, code, name, target_track):
    edit_id = client.request("POST", "/edits", {})["id"]
    try:
        prefix = "/edits/" + edit_id
        tracks = client.request("GET", prefix + "/tracks").get("tracks", [])
        bundles = client.request("GET", prefix + "/bundles").get("bundles", [])
        return already_uploaded(tracks, bundles, code, name, target_track)
    finally:
        # Inspection uses an uncommitted edit and never changes a release.
        client.request("DELETE", "/edits/" + edit_id)


def write_outputs(values):
    with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as output:
        for key, value in values.items():
            output.write("{}={}\n".format(key, value))


def main():
    if len(sys.argv) != 2:
        raise ValueError("Expected metadata or preflight command.")
    if sys.argv[1] == "metadata":
        target = release_target(os.environ.get("GITHUB_REF", ""))
        write_outputs({**target, **release_metadata(
            os.environ.get("PLAY_VERSION_CODE_BASE", ""),
            os.environ.get("GITHUB_RUN_NUMBER", ""),
            os.environ.get("GITHUB_SHA", ""),
        )})
    elif sys.argv[1] == "preflight":
        code = int(os.environ["RELEASE_VERSION_CODE"])
        target = release_target(os.environ.get("GITHUB_REF", ""))
        if os.environ.get("PLAY_TRACK") != target["track"]:
            raise ValueError("Selected Play track does not match the release branch.")
        found = check_play(PlayClient(os.environ.get("PLAY_ACCESS_TOKEN", "")), code, os.environ["RELEASE_NAME"], target["track"])
        write_outputs({"already_uploaded": str(found).lower()})
        print("Matching release already uploaded; skipping build and upload." if found else "Play version check passed.")
    else:
        raise ValueError("Expected metadata or preflight command.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, RuntimeError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
    except Exception:
        print("Release preflight failed; check the workflow configuration and Play API response schema.", file=sys.stderr)
        sys.exit(1)
