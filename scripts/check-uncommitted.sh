#!/usr/bin/env bash
# feature 브랜치에 커밋 안 된 변경사항이 있으면 Claude가 알아서
# 커밋 -> push -> PR 생성/갱신을 하도록 종료를 막고 지시한다.
cd "$(git rev-parse --show-toplevel)" || exit 0

branch=$(git branch --show-current)

if [ -z "$branch" ] || [ "$branch" = "main" ] || [ "$branch" = "master" ]; then
  exit 0
fi

if [ -n "$(git status --porcelain)" ]; then
  echo "커밋되지 않은 변경사항이 있습니다. 사용자에게 다시 묻지 말고 다음을 수행하세요:" >&2
  echo "1) git status/diff로 변경 내용 검토 (민감정보 파일은 add하지 말 것)" >&2
  echo "2) git add 후 '[타입] 설명' 컨벤션으로 커밋 (CLAUDE.md 참고)" >&2
  echo "3) git push -u origin $branch" >&2
  echo "4) gh pr view $branch --json url -q .url 로 PR 존재 확인 -> 없으면 gh pr create --fill 로 생성, 있으면 push만으로 충분" >&2
  exit 2
fi

exit 0
