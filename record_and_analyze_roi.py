import subprocess
import time
import os
import sys
from pathlib import Path
from PIL import Image, ImageChops, ImageStat

DEVICE = "192.168.100.26:5555"
OUT_DIR = str(Path(__file__).resolve().parent / "technique_videos")
ARTIFACT_DIR = os.environ.get("KONOHA_ARTIFACT_DIR", str(Path(__file__).resolve().parent / "verification" / "comparisons"))
os.makedirs(OUT_DIR, exist_ok=True)
os.makedirs(ARTIFACT_DIR, exist_ok=True)

TECHNIQUES = [
    ("rasengan", "Расенґан"),
    ("chidori", "Чідорі"),
    ("sharingan", "Шарінґан"),
    ("kurama", "Чакра Курами"),
    ("summoning", "Виклик жаби"),
    ("hiraishin", "Хірайшин"),
    ("fireball", "Вогняна куля"),
    ("amaterasu", "Аматерасу"),
    ("rasenshuriken", "Расенсюрикен"),
    ("gamabunta", "Стрибок Гамабунти")
]

def run_adb(cmd, timeout=30):
    full_cmd = f"adb -s {DEVICE} {cmd}"
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True, timeout=timeout)
    return res.stdout.strip()

print(f"Connecting to {DEVICE}...")
subprocess.run(f"adb connect {DEVICE}", shell=True, capture_output=True)

pref_template = """<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="technique">{tech}</string>
    <boolean name="motion" value="true" />
    <string name="pins">[&quot;com.phlox.tvwebbrowser&quot;,&quot;org.smarttube.stable&quot;,&quot;org.xbmc.kodi&quot;,&quot;top.rootu.lampa&quot;,&quot;ua.kino&quot;,&quot;com.android.documentsui&quot;]</string>
    <int name="runner_best_naruto" value="232" />
    <string name="scene">dusk</string>
</map>
"""

# Card 1 ROI on 1366x768 Lenovo TV screen:
ROI_CARD1 = (50, 500, 280, 740)
ROI_CARD2 = (235, 500, 445, 740)

def count_color_signature(img_crop, tech):
    width, height = img_crop.size
    pixels = img_crop.load()
    matches = 0
    for x in range(width):
        for y in range(height):
            r, g, b = pixels[x, y][:3]
            if tech in ("rasengan", "rasenshuriken"):
                # Cyan vortex: high blue & green, lower red
                if b > 170 and g > 150 and r < 140:
                    matches += 1
            elif tech == "chidori":
                # Electric blue / white spark: high blue, bright
                if (b > 200 and g > 150) or (r > 210 and g > 220 and b > 230):
                    matches += 1
            elif tech == "sharingan":
                # Tomoe crimson red
                if r > 140 and g < 70 and b < 70:
                    matches += 1
            elif tech == "kurama":
                # Golden / flame tails: high red & green, low blue
                if r > 180 and g > 110 and b < 80:
                    matches += 1
            elif tech in ("fireball", "amaterasu"):
                # Flame / dark flame: red/orange or violet
                if (r > 170 and g > 60 and b < 80) or (r > 80 and b > 100 and g < 70):
                    matches += 1
            elif tech in ("summoning", "hiraishin"):
                # Seal glyphs / gold flashes
                if (r > 170 and g > 150 and b < 110) or (r < 110 and g > 110 and b < 95):
                    matches += 1
            elif tech == "gamabunta":
                # Toad sprite / shadow / dust
                if (r > 130 and g > 75 and b < 75) or (r > 160 and g > 150 and b > 120):
                    matches += 1
            else:
                matches += 1
    return matches

results = []

for tech_id, tech_name in TECHNIQUES:
    print(f"\n==========================================")
    print(f"Testing technique: {tech_id.upper()} ({tech_name})")
    print(f"==========================================")
    
    local_pref = os.path.join(OUT_DIR, "temp_pref.xml")
    with open(local_pref, "w", encoding="utf-8") as f:
        f.write(pref_template.format(tech=tech_id))
    
    # 1. Apply preferences to TV
    run_adb(f"push {local_pref} /data/local/tmp/prefs.xml")
    run_adb("shell run-as ua.konoha.tv.nativeparity cp /data/local/tmp/prefs.xml shared_prefs/konoha_preferences_v1.xml")
    
    # 2. Restart app
    run_adb("shell am force-stop ua.konoha.tv.nativeparity")
    time.sleep(0.5)
    run_adb("shell am start -n ua.konoha.tv.nativeparity/ua.konoha.tv.nativepreview.MainActivity")
    time.sleep(2.5) # Wait for cold launch and focus restore on Card 1
    
    # 3. Record video (4 seconds, keyevent DPAD_RIGHT at 1.8s)
    dev_video = f"/sdcard/tech_{tech_id}.mp4"
    local_video = os.path.join(OUT_DIR, f"tech_{tech_id}.mp4")
    
    print(f"Recording 4s on TV (Card 1 idle -> DPAD_RIGHT navigation)...")
    record_cmd = f"adb -s {DEVICE} shell \"screenrecord --time-limit 4 {dev_video} & sleep 1.8; input keyevent 22; sleep 1.5\""
    record_proc = subprocess.Popen(record_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    record_proc.wait(timeout=15)
    time.sleep(1.0)
    
    # 4. Pull video
    run_adb(f"pull {dev_video} \"{local_video}\"")
    size = os.path.getsize(local_video) if os.path.exists(local_video) else 0
    print(f"Video captured: {size} bytes -> {local_video}")
    
    # 5. Extract frames:
    # Frame 1: 0.8s (idle, aura active on card 1)
    # Frame 2: 1.4s (idle, 600ms later - testing idle motion delta)
    # Frame 3: 2.7s (after DPAD_RIGHT - card 2 focused)
    frame1 = os.path.join(OUT_DIR, f"{tech_id}_01_idle1.png")
    frame2 = os.path.join(OUT_DIR, f"{tech_id}_02_idle2.png")
    frame3 = os.path.join(OUT_DIR, f"{tech_id}_03_nav.png")
    
    subprocess.run(f"ffmpeg -y -ss 00:00:00.800 -i \"{local_video}\" -frames:v 1 \"{frame1}\"", shell=True, capture_output=True)
    subprocess.run(f"ffmpeg -y -ss 00:00:01.400 -i \"{local_video}\" -frames:v 1 \"{frame2}\"", shell=True, capture_output=True)
    subprocess.run(f"ffmpeg -y -ss 00:00:02.700 -i \"{local_video}\" -frames:v 1 \"{frame3}\"", shell=True, capture_output=True)
    
    if not (os.path.exists(frame1) and os.path.exists(frame2) and os.path.exists(frame3)):
        print(f"ERROR: Missing extracted frames for {tech_id}")
        continue
    
    img1 = Image.open(frame1).convert("RGB")
    img2 = Image.open(frame2).convert("RGB")
    img3 = Image.open(frame3).convert("RGB")
    
    # STRICT ROI CROPPING:
    roi1 = img1.crop(ROI_CARD1)
    roi2 = img2.crop(ROI_CARD1)
    roi3 = img3.crop(ROI_CARD1)
    roi3_c2 = img3.crop(ROI_CARD2)
    
    # Save ROI crops
    roi1.save(os.path.join(OUT_DIR, f"{tech_id}_roi_idle1.png"))
    roi2.save(os.path.join(OUT_DIR, f"{tech_id}_roi_idle2.png"))
    roi3.save(os.path.join(OUT_DIR, f"{tech_id}_roi_nav_c1.png"))
    roi3_c2.save(os.path.join(OUT_DIR, f"{tech_id}_roi_nav_c2.png"))
    
    # Save proof to artifact directory
    roi1.save(os.path.join(ARTIFACT_DIR, f"{tech_id}_roi_idle1.png"))
    roi2.save(os.path.join(ARTIFACT_DIR, f"{tech_id}_roi_idle2.png"))
    
    # Check color signature in ROI
    color_pixels = count_color_signature(roi1, tech_id)
    
    # Check strict pixel delta between frame 1 and frame 2 in the ROI (IDLE ANIMATION)
    diff_roi = ImageChops.difference(roi1, roi2)
    stat_roi = ImageStat.Stat(diff_roi)
    roi_mean = sum(stat_roi.mean) / 3.0
    
    diff_gray = diff_roi.convert("L")
    hist = diff_gray.histogram()
    # pixels with delta > 10 (ignoring H.264 compression noise)
    changed_roi_pixels = sum(hist[11:])
    total_roi_pixels = roi1.width * roi1.height
    pct_roi = (changed_roi_pixels / total_roi_pixels) * 100.0
    
    diff_roi.save(os.path.join(OUT_DIR, f"{tech_id}_roi_diff.png"))
    diff_roi.save(os.path.join(ARTIFACT_DIR, f"{tech_id}_roi_diff.png"))
    
    # Navigation diff (card 1 unhovered, aura moves to card 2)
    diff_nav = ImageChops.difference(roi1, roi3)
    stat_nav = ImageStat.Stat(diff_nav)
    nav_mean = sum(stat_nav.mean) / 3.0
    
    has_idle_anim = (changed_roi_pixels > 80 and roi_mean > 0.25)
    has_nav_anim = nav_mean > 3.0
    
    res = {
        "tech_id": tech_id,
        "tech_name": tech_name,
        "video_path": local_video,
        "video_size": size,
        "color_pixels": color_pixels,
        "roi_mean": round(roi_mean, 3),
        "changed_roi_pixels": changed_roi_pixels,
        "pct_roi": round(pct_roi, 2),
        "nav_mean": round(nav_mean, 3),
        "idle_anim": has_idle_anim,
        "nav_anim": has_nav_anim
    }
    results.append(res)
    print(f"[{tech_id.upper()}] Color signature pixels: {color_pixels} | ROI delta mean: {roi_mean:.3f} | Changed ROI pixels: {changed_roi_pixels} ({pct_roi:.2f}%) | Nav Delta: {nav_mean:.2f}")

print("\n" + "="*120)
print(f"{'TECHNIQUE':<18} | {'COLOR PIX':<10} | {'ROI MEAN':<10} | {'CHANGED PIX':<14} | {'ROI %':<8} | {'IDLE ANIM?':<12} | {'NAV ANIM?':<10}")
print("="*120)
for r in results:
    idle_str = "ТАК (АКТИВНА)" if r["idle_anim"] else "НІ"
    nav_str = "ТАК (АКТИВНА)" if r["nav_anim"] else "НІ"
    print(f"{r['tech_name'] + ' (' + r['tech_id'] + ')':<18} | {r['color_pixels']:<10} | {r['roi_mean']:<10} | {r['changed_roi_pixels']:<14} | {r['pct_roi']:<8}% | {idle_str:<12} | {nav_str:<10}")
print("="*120)
