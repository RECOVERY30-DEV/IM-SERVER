#!/usr/bin/env bash
# 최초 1회 실행: 저장소의 git hooks 경로를 .githooks 로 지정한다.
set -e
cd "$(git rev-parse --show-toplevel)"
git config core.hooksPath .githooks
echo "git hooks 경로가 .githooks 로 설정되었습니다."
