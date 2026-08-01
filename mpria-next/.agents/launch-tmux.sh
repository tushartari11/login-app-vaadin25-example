#!/usr/bin/env bash
#
# Launch the three-agent build team, one Claude Code session per tmux window.
#
#   ./.agents/launch-tmux.sh          create the session and attach
#   ./.agents/launch-tmux.sh --detach create it in the background
#   tmux attach -t mpria              attach later
#   tmux kill-session -t mpria        tear the whole team down
#
# Windows:
#   0 config   services/config-service      (the administration application)
#   1 bff      services/presentation-bff    (the presentation-model layer)
#   2 web      web/                         (the generic renderer)
#   3 ops      a plain shell for builds, docker and git
#
# Each agent gets its brief from .agents/NN-*.md, which points at .agents/SHARED-BRIEF.md.
# The three work in disjoint directories, so they do not collide.

set -euo pipefail

SESSION="mpria"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA21="/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home"

if ! command -v tmux >/dev/null 2>&1; then
    echo "tmux is not installed. brew install tmux" >&2
    exit 1
fi
if ! command -v claude >/dev/null 2>&1; then
    echo "the claude CLI is not on PATH" >&2
    exit 1
fi

if tmux has-session -t "$SESSION" 2>/dev/null; then
    echo "Session '$SESSION' already exists. Attach with:  tmux attach -t $SESSION"
    echo "Or tear it down with:                            tmux kill-session -t $SESSION"
    exit 0
fi

# Permissions: edits are pre-approved inside the repo, and the build commands each agent needs are
# allowlisted. Deliberately NOT --dangerously-skip-permissions — an agent that wants to do anything
# outside this list should have to ask, and you should see it ask.
ALLOWED=(
    --allowedTools
    'Bash(mvn:*)'
    'Bash(npm:*)'
    'Bash(npx:*)'
    'Bash(node:*)'
    'Bash(ls:*)'
    'Bash(cat:*)'
    'Bash(find:*)'
    'Bash(grep:*)'
    'Bash(unzip:*)'
    'Bash(mkdir:*)'
    'Read' 'Write' 'Edit' 'Glob' 'Grep'
)

launch_agent() {
    local window_index="$1" window_name="$2" brief="$3"

    if [[ ! -f "$ROOT/$brief" ]]; then
        echo "missing brief: $brief" >&2
        exit 1
    fi

    tmux new-window -t "$SESSION:$window_index" -n "$window_name" -c "$ROOT"
    tmux send-keys -t "$SESSION:$window_index" \
        "export JAVA_HOME=$JAVA21 && clear && claude --permission-mode acceptEdits $(printf '%q ' "${ALLOWED[@]}")\"\$(cat $brief)\"" \
        C-m
}

# Window 0 is created with the session; make it the ops shell and move the agents after it.
tmux new-session -d -s "$SESSION" -n "ops" -c "$ROOT"
tmux send-keys -t "$SESSION:0" "export JAVA_HOME=$JAVA21 && clear && echo 'ops shell — builds, docker, git'" C-m

launch_agent 1 "config" ".agents/01-config-service.md"
launch_agent 2 "bff"    ".agents/02-presentation-bff.md"
launch_agent 3 "web"    ".agents/03-web-renderer.md"

tmux select-window -t "$SESSION:0"

cat <<EOF
Started tmux session '$SESSION' with three agents:

  window 1  config   -> services/config-service
  window 2  bff      -> services/presentation-bff
  window 3  web      -> web/
  window 0  ops      -> plain shell

  tmux attach -t $SESSION      attach
  Ctrl-b <n>                   switch to window n
  Ctrl-b d                     detach, leaving the agents running
  tmux kill-session -t $SESSION  stop everything
EOF

if [[ "${1:-}" != "--detach" ]]; then
    tmux attach -t "$SESSION"
fi
