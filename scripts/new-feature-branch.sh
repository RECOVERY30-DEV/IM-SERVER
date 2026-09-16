#!/usr/bin/env bash
# main 기준으로 feature/<기능명> 브랜치를 생성하고 이동한다.
set -e

if [ -z "$1" ]; then
  echo "사용법: ./scripts/new-feature-branch.sh <기능명>" >&2
  exit 1
fi

cd "$(git rev-parse --show-toplevel)"
git checkout main
git pull origin main
git checkout -b "feature/$1"
echo "feature/$1 브랜치를 생성하고 이동했습니다."
