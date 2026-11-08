#!/usr/bin/env bash
# Claude v14 review: compact UI probe across viewports/scenes (private session, prefs volume=0, audio/ambience off)
export AGENT_BROWSER_SESSION=konoha-claude-v14-art-review
E="$(dirname "$0")"
for vp in "960 360" "960 480" "960 540" "960 620" "1280 600"; do set -- $vp
 agent-browser set viewport $1 $2 >/dev/null
 for s in sai jiraiya minato; do
  agent-browser eval "(()=>{const p=JSON.parse(localStorage.getItem('konoha.preferences.v1'));Object.assign(p,{scene:'$s',audio:false,ambience:false,volume:0,musicVolume:0});localStorage.setItem('konoha.preferences.v1',JSON.stringify(p));return 1})()" >/dev/null
  agent-browser reload >/dev/null; agent-browser wait 2200 >/dev/null
  agent-browser screenshot "$E/compact-$s-${1}x${2}.png" >/dev/null
  echo "== $s ${1}x${2}"; agent-browser eval --stdin < "$E/probe-compact.js"
 done
done
