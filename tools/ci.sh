#!/usr/bin/env bash
# Push HEAD to main, wait for its CI build, and print a short result:
#   build <success|failure> (<run id>) for <commit>
#   (on failure) the Kotlin/Gradle error lines
#   apk updated <time>
# One call instead of polling. Run it in the background or with a ~11 min timeout.
set -uo pipefail
REPO=contactvirgilio/LemuroidPlusFork
git push -q origin HEAD:main 2>&1 | grep -viE "moved|location|please use|remote:" || true
SHA=$(git rev-parse HEAD)
ID=""
for _ in $(seq 1 30); do
  ID=$(gh run list -R "$REPO" --limit 5 --json databaseId,headSha -q ".[]|select(.headSha==\"$SHA\")|.databaseId" | head -1)
  [ -n "$ID" ] && break
  sleep 5
done
[ -z "$ID" ] && { echo "no CI run found for ${SHA:0:7}"; exit 1; }
gh run watch "$ID" -R "$REPO" --exit-status > /dev/null 2>&1
CONCL=$(gh run view "$ID" -R "$REPO" --json conclusion -q .conclusion)
echo "build $CONCL ($ID) for ${SHA:0:7}"
if [ "$CONCL" != success ]; then
  gh run view "$ID" -R "$REPO" --log-failed 2>/dev/null | grep -E " e: |error:|What went wrong" -A2 | cut -c1-220 | head -20
  exit 1
fi
gh api "repos/$REPO/releases/tags/latest-build" \
  -q '.assets[]|select(.name=="LemuroidPlus-latest.apk")|"apk updated "+.updated_at'
