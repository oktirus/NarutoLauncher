import subprocess
import time
import os
import sys
from pathlib import Path

TECHNIQUES = [
    "rasengan",
    "chidori",
    "sharingan",
    "kurama",
    "summoning",
    "hiraishin",
    "fireball",
    "amaterasu",
    "rasenshuriken",
    "gamabunta"
]

DEVICE = "192.168.100.26:5555"
OUT_DIR = str(Path(__file__).resolve().parent / "technique_videos")
os.makedirs(OUT_DIR, exist_ok=True)

def run_adb(cmd, timeout=30):
    full_cmd = f"adb -s {DEVICE} {cmd}"
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True, timeout=timeout)
    return res.stdout.strip()

print(f"Connecting to {DEVICE}...")
subprocess.run(f"adb connect {DEVICE}", shell=True, capture_output=True)

pref_template = """<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="technique">{tech}</string>
    <string name="pins">[&quot;com.phlox.tvwebbrowser&quot;,&quot;org.smarttube.stable&quot;,&quot;org.xbmc.kodi&quot;,&quot;top.rootu.lampa&quot;,&quot;ua.kino&quot;,&quot;com.android.documentsui&quot;]</string>
    <int name="runner_best_naruto" value="232" />
    <string name="scene">dusk</string>
</map>
"""

results = []

for tech in TECHNIQUES:
    print(f"\n--- Testing technique: {tech} ---")
    local_pref = os.path.join(OUT_DIR, "temp_pref.xml")
    with open(local_pref, "w", encoding="utf-8") as f:
        f.write(pref_template.format(tech=tech))
    
    # Push and apply prefs
    run_adb(f"push {local_pref} /data/local/tmp/prefs.xml")
    run_adb("shell run-as ua.konoha.tv.nativeparity cp /data/local/tmp/prefs.xml shared_prefs/konoha_preferences_v1.xml")
    
    # Restart app
    run_adb("shell am force-stop ua.konoha.tv.nativeparity")
    time.sleep(0.5)
    run_adb("shell am start -n ua.konoha.tv.nativeparity/ua.konoha.tv.nativepreview.MainActivity")
    time.sleep(2.5) # Wait for layout & initial focus
    
    # Record 3.5 seconds with navigation
    dev_video = f"/sdcard/tech_{tech}.mp4"
    local_video = os.path.join(OUT_DIR, f"tech_{tech}.mp4")
    
    print(f"Recording video on TV for {tech}...")
    # Trigger screenrecord in background on device, then sleep, press DPAD_RIGHT, sleep
    # We execute screenrecord with 4 seconds
    record_proc = subprocess.Popen(
        f"adb -s {DEVICE} shell \"screenrecord --time-limit 4 {dev_video} & sleep 1.5; input keyevent 22; sleep 1.5\"",
        shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE
    )
    record_proc.wait(timeout=15)
    time.sleep(1.0)
    
    # Pull video
    run_adb(f"pull {dev_video} \"{local_video}\"")
    
    size = os.path.getsize(local_video) if os.path.exists(local_video) else 0
    print(f"Video {tech}.mp4 size: {size} bytes")
    
    # Extract 3 frames: frame_01 (at 0.8s, idle), frame_02 (at 1.2s, idle), frame_03 (at 2.2s, during/after nav)
    frame1 = os.path.join(OUT_DIR, f"{tech}_01_idle1.png")
    frame2 = os.path.join(OUT_DIR, f"{tech}_02_idle2.png")
    frame3 = os.path.join(OUT_DIR, f"{tech}_03_nav.png")
    
    subprocess.run(f"ffmpeg -y -ss 00:00:00.800 -i \"{local_video}\" -frames:v 1 \"{frame1}\"", shell=True, capture_output=True)
    subprocess.run(f"ffmpeg -y -ss 00:00:01.300 -i \"{local_video}\" -frames:v 1 \"{frame2}\"", shell=True, capture_output=True)
    subprocess.run(f"ffmpeg -y -ss 00:00:02.300 -i \"{local_video}\" -frames:v 1 \"{frame3}\"", shell=True, capture_output=True)
    
    results.append({
        "tech": tech,
        "size": size,
        "video": local_video,
        "f1": frame1,
        "f2": frame2,
        "f3": frame3
    })

print("\n--- All 10 techniques recorded! Analyzing frames... ---")

from PIL import Image, ImageChops, ImageStat

summary = []

for r in results:
    tech = r["tech"]
    f1_path = r["f1"]
    f2_path = r["f2"]
    f3_path = r["f3"]
    
    if not (os.path.exists(f1_path) and os.path.exists(f2_path) and os.path.exists(f3_path)):
        summary.append({
            "tech": tech,
            "status": "MISSING_FRAMES",
            "size": r["size"]
        })
        continue

    img1 = Image.open(f1_path).convert("RGB")
    img2 = Image.open(f2_path).convert("RGB")
    img3 = Image.open(f3_path).convert("RGB")

    # Crop to dock/emblem area (x: 200..1200, y: 450..760) where focus technique lives
    box = (150, 420, 1250, 760)
    crop1 = img1.crop(box)
    crop2 = img2.crop(box)
    crop3 = img3.crop(box)

    # 1. Idle animation difference (crop1 vs crop2)
    diff_idle = ImageChops.difference(crop1, crop2)
    stat_idle = ImageStat.Stat(diff_idle)
    mean_idle = sum(stat_idle.mean) / 3.0
    max_idle = max(stat_idle.extrema[0][1], stat_idle.extrema[1][1], stat_idle.extrema[2][1])
    
    # Count pixels with significant delta (> 8)
    diff_gray = diff_idle.convert("L")
    hist = diff_gray.histogram()
    changed_pixels_idle = sum(hist[9:])
    total_pixels = crop1.width * crop1.height
    pct_idle = (changed_pixels_idle / total_pixels) * 100.0

    # 2. Navigation animation difference (crop2 vs crop3)
    diff_nav = ImageChops.difference(crop2, crop3)
    stat_nav = ImageStat.Stat(diff_nav)
    mean_nav = sum(stat_nav.mean) / 3.0
    max_nav = max(stat_nav.extrema[0][1], stat_nav.extrema[1][1], stat_nav.extrema[2][1])
    pct_nav = (sum(diff_nav.convert("L").histogram()[9:]) / total_pixels) * 100.0

    # Save visual diff image
    diff_idle.save(os.path.join(OUT_DIR, f"{tech}_diff_idle.png"))
    diff_nav.save(os.path.join(OUT_DIR, f"{tech}_diff_nav.png"))

    has_idle_anim = changed_pixels_idle > 200 and mean_idle > 0.08
    has_nav_anim = pct_nav > 2.0 and mean_nav > 1.0

    summary.append({
        "tech": tech,
        "size_bytes": r["size"],
        "mean_idle": round(mean_idle, 3),
        "max_idle": max_idle,
        "changed_pct_idle": round(pct_idle, 2),
        "mean_nav": round(mean_nav, 3),
        "changed_pct_nav": round(pct_nav, 2),
        "idle_anim": has_idle_anim,
        "nav_anim": has_nav_anim
    })

print("\n" + "="*85)
print(f"{'TECHNIQUE':<15} | {'SIZE (MB)':<10} | {'IDLE MEAN':<10} | {'IDLE PIX %':<11} | {'IDLE ANIM?':<11} | {'NAV ANIM?':<10}")
print("="*85)
for s in summary:
    size_mb = f"{s['size_bytes'] / (1024*1024):.2f}"
    idle_str = "YES (ACTIVE)" if s.get("idle_anim") else "NO (STATIC)"
    nav_str = "YES (ACTIVE)" if s.get("nav_anim") else "NO"
    print(f"{s['tech']:<15} | {size_mb:<10} | {s.get('mean_idle', 0):<10} | {s.get('changed_pct_idle', 0):<10}% | {idle_str:<11} | {nav_str:<10}")
print("="*85)

