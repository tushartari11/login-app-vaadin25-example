#!/usr/bin/env bash
#
# Open one terminal window per agent.
#
# The agents all live in the single tmux session created by launch-tmux.sh. Attaching three
# terminals to that one session would mirror the same window in all three — tmux clients on the
# same session share a current window. So this script creates three *grouped* sessions
# (`new-session -t mpria`): they share the window list but each keeps its own current window,
# which is what lets three terminals show three different agents at once.
#
#   ./.agents/open-windows.sh            open in iTerm if present, else Terminal.app
#   ./.agents/open-windows.sh Terminal   force Terminal.app
#
# Closing a window just detaches it; the agent keeps running. Reopen with this script.

set -euo pipefail

SESSION="mpria"

if ! tmux has-session -t "$SESSION" 2>/dev/null; then
    echo "No '$SESSION' session. Run ./.agents/launch-tmux.sh --detach first." >&2
    exit 1
fi

APP="${1:-}"
if [[ -z "$APP" ]]; then
    if [[ -d /Applications/iTerm.app ]]; then APP="iTerm"; else APP="Terminal"; fi
fi

# Absolute path, deliberately. A terminal launched via AppleScript with an explicit command does
# not run a login shell, so Homebrew's /opt/homebrew/bin is not on PATH and a bare `tmux` fails —
# the window opens and closes again with no visible error.
TMUX_BIN="$(command -v tmux)"

# view session name : window index in the shared group
VIEWS=(
    "agent-config:1"
    "agent-bff:2"
    "agent-web:3"
)

for view in "${VIEWS[@]}"; do
    name="${view%%:*}"
    window="${view##*:}"

    # A grouped session shares mpria's windows but tracks its own current window.
    if ! tmux has-session -t "$name" 2>/dev/null; then
        tmux new-session -d -s "$name" -t "$SESSION"
    fi
    # Do not let this view follow the others when they switch windows.
    tmux set-option -t "$name" destroy-unattached off >/dev/null 2>&1 || true
    tmux select-window -t "$name:$window"

    case "$APP" in
        iTerm)
            osascript <<OSA >/dev/null
tell application "iTerm"
    create window with default profile command "$TMUX_BIN attach-session -t $name"
end tell
OSA
            ;;
        Terminal)
            osascript <<OSA >/dev/null
tell application "Terminal"
    do script "$TMUX_BIN attach-session -t $name"
    activate
end tell
OSA
            ;;
        *)
            echo "unsupported terminal: $APP (expected iTerm or Terminal)" >&2
            exit 1
            ;;
    esac
    sleep 1
done

echo "Opened three $APP windows:"
echo "  agent-config -> services/config-service"
echo "  agent-bff    -> services/presentation-bff"
echo "  agent-web    -> web/"
echo
echo "Closing a window only detaches it; the agent keeps running."
echo "Stop everything with: tmux kill-session -t $SESSION"
