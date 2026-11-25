#!/usr/bin/env python3
"""
Generates recorded voice clips for every sentence the app speaks, using Gemini text-to-speech.

- Reads app/src/main/assets/content.json and collects every spoken sentence.
- Clip file name = first 16 hex chars of SHA-1 of the exact text + ".ogg" (must match Speaker.clipName).
- Only generates clips that don't exist yet, so it can resume day after day within the free quota.
- Keys come from the GEMINI_KEYS environment variable (one per line); it rotates when one hits its limit.

Run in GitHub Actions; needs ffmpeg. No third-party Python packages.
"""
import base64, hashlib, json, os, re, subprocess, sys, tempfile, time, urllib.error, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTENT = os.path.join(ROOT, "app/src/main/assets/content.json")
OUT = os.path.join(ROOT, "app/src/main/assets/voice")
BUDGET_SEC = int(os.environ.get("VOICE_BUDGET_SEC", "1500"))
VOICE = os.environ.get("VOICE_NAME", "Kore")
MODELS = ["gemini-3.1-flash-tts-preview", "gemini-2.5-flash-preview-tts", "gemini-2.5-flash-tts"]
PROMPT = ("Read the following text aloud exactly as written, for a young child, "
          "with a warm, gentle, slightly slow voice. Arabic parts in clear Modern Standard Arabic (فصحى), "
          "following the tashkeel exactly; English words and letters in clear native English pronunciation. "
          "Read only the text, nothing else:\n\n")


def clip_name(text):
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:16] + ".ogg"


def sentences():
    d = json.load(open(CONTENT, encoding="utf-8"))
    out = list(d.get("phrases", {}).values())
    # Placement test first: it's the first thing every child hears.
    for k in d.get("placement", {}):
        out += [q["exercise"]["speak"] or q["exercise"]["prompt"] for q in d["placement"][k]]
    for s in d["subjects"]:
        for lv in s["levels"]:
            for l in lv["lessons"]:
                out.append(l["introSpeak"])
                out += [e["speak"] or e["prompt"] for e in l["exercises"]]
    for k in d.get("placement", {}):
        out += [q["exercise"]["speak"] or q["exercise"]["prompt"] for q in d["placement"][k]]
    for st in d.get("stories", []):
        out += st["pages"]
    seen, uniq = set(), []
    for t in out:
        if t and t.strip() and t not in seen:
            seen.add(t)
            uniq.append(t)
    return uniq


def spoken_form(text):
    # Drop emoji / symbols / direction marks; keep letters, tashkeel, digits, punctuation.
    t = "".join(c for c in text if not (0x2066 <= ord(c) <= 0x2069 or ord(c) >= 0x2190 and not (0x0600 <= ord(c) <= 0x06FF)))
    return re.sub(r"\s+", " ", t.replace("…", "، ")).strip()


def endpoints(key, model):
    body_url_g = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
    body_url_v = f"https://aiplatform.googleapis.com/v1/publishers/google/models/{model}:generateContent?key={key}"
    return [("gemini", body_url_g, {"x-goog-api-key": key}), ("vertex", body_url_v, {})]


def call(url, headers, text):
    body = {
        "contents": [{"role": "user", "parts": [{"text": PROMPT + text}]}],
        "generationConfig": {
            "responseModalities": ["AUDIO"],
            "speechConfig": {"voiceConfig": {"prebuiltVoiceConfig": {"voiceName": VOICE}}},
        },
    }
    req = urllib.request.Request(url, data=json.dumps(body).encode(), method="POST",
                                 headers={"Content-Type": "application/json", **headers})
    with urllib.request.urlopen(req, timeout=120) as r:
        data = json.load(r)
    part = data["candidates"][0]["content"]["parts"][0]["inlineData"]
    m = re.search(r"rate=(\d+)", part.get("mimeType", ""))
    return base64.b64decode(part["data"]), int(m.group(1)) if m else 24000


def to_ogg(pcm, rate, dest):
    with tempfile.NamedTemporaryFile(suffix=".pcm", delete=False) as f:
        f.write(pcm)
        src = f.name
    trim = ("silenceremove=start_periods=1:start_threshold=-45dB,areverse,"
            "silenceremove=start_periods=1:start_threshold=-45dB,areverse,"
            "apad=pad_dur=0.15")
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "s16le", "-ar", str(rate), "-ac", "1",
                    "-i", src, "-af", trim, "-c:a", "libopus", "-b:a", "24k", "-application", "voip", dest],
                   check=True)
    os.unlink(src)


def main():
    keys = [k.strip() for k in os.environ.get("GEMINI_KEYS", "").splitlines() if k.strip()]
    if not keys:
        print("No GEMINI_KEYS secret; skipping voice generation.")
        return
    os.makedirs(OUT, exist_ok=True)
    todo = [t for t in sentences() if not os.path.exists(os.path.join(OUT, clip_name(t)))]
    print(f"{len(todo)} clips to generate")
    start = time.time()
    route = None            # (model, endpoint kind) that worked
    exhausted = set()       # keys that hit their limit
    ki = 0
    made = 0
    for text in todo:
        if time.time() - start > BUDGET_SEC:
            print("Time budget used; will continue next run.")
            break
        done = False
        while not done and len(exhausted) < len(keys):
            key = keys[ki % len(keys)]
            if key in exhausted:
                ki += 1
                continue
            combos = [route] if route else [(m, k) for m in MODELS for k in ("gemini", "vertex")]
            last_err = None
            for model, kind in combos:
                url, headers = next((u, h) for (n, u, h) in endpoints(key, model) if n == kind)
                try:
                    pcm, rate = call(url, headers, spoken_form(text))
                    to_ogg(pcm, rate, os.path.join(OUT, clip_name(text)))
                    if route is None:
                        print(f"Using model={model} via {kind}")
                    route = (model, kind)
                    made += 1
                    done = True
                    break
                except urllib.error.HTTPError as e:
                    msg = e.read().decode("utf-8", "ignore")[:1500]
                    last_err = (e.code, msg)
                    if e.code == 429:
                        break
                except Exception as e:  # bad response, ffmpeg, timeout…
                    last_err = (0, str(e)[:300])
            if done:
                time.sleep(1.0)
                break
            code = last_err[0] if last_err else 0
            if code == 429:
                msg = last_err[1]
                if "PerDay" in msg or "per day" in msg.lower() or "daily" in msg.lower():
                    print(f"Key #{keys.index(key) + 1} hit its DAILY limit: {msg[:200]}")
                    exhausted.add(key)
                    ki += 1
                else:
                    # Per-minute limit: try the next key, and if all were just used, wait for the window.
                    ki += 1
                    m = re.search(r'retryDelay"\s*:\s*"(\d+)', msg)
                    wait = int(m.group(1)) + 1 if m else 20
                    if ki % len(keys) == 0:
                        print(f"Per-minute limit on all keys; waiting {wait}s ({made} made so far)")
                        time.sleep(wait)
            elif code in (400, 401, 403) and route is None:
                print(f"Key #{keys.index(key) + 1} not accepted: {last_err}")
                exhausted.add(key)
                ki += 1
            else:
                print(f"Skipping one sentence after error: {last_err}")
                break
            if time.time() - start > BUDGET_SEC:
                break
        if len(exhausted) >= len(keys):
            print("All keys are at their limit for now; will continue next run.")
            break
    left = len([t for t in sentences() if not os.path.exists(os.path.join(OUT, clip_name(t)))])
    print(f"Generated {made} clips this run; {left} still missing.")


if __name__ == "__main__":
    main()
