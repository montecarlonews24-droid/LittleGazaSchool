#!/usr/bin/env python3
"""Generates app/src/main/assets/content.json for Little Gaza School."""
import json, random, sys

rnd = random.Random(2026)
RLI, PDI = "\u2067", "\u2069"   # equations read right-to-left, Arabic style

def ex(type_, prompt, options, answer_value, speak="", show="", count=0, count_emoji=""):
    opts = list(options)
    assert answer_value in opts, (prompt, answer_value, opts)
    assert len(set(opts)) == len(opts), (prompt, opts)
    rnd.shuffle(opts)
    return {"type": type_, "prompt": prompt, "speak": speak, "show": show,
            "count": count, "countEmoji": count_emoji,
            "options": opts, "answer": opts.index(answer_value)}

def lesson(lid, title, icon, big, text, speak, exercises):
    return {"id": lid, "title": title, "icon": icon, "introBig": big,
            "introText": text, "introSpeak": speak, "exercises": exercises}

# ---------------------------------------------------------------- Arabic
LETTERS = [
    ("ا", "ألف", "أرنب", "🐰"), ("ب", "باء", "بطة", "🦆"), ("ت", "تاء", "تفاحة", "🍎"),
    ("ث", "ثاء", "ثعلب", "🦊"), ("ج", "جيم", "جمل", "🐫"), ("ح", "حاء", "حصان", "🐴"),
    ("خ", "خاء", "خروف", "🐑"), ("د", "دال", "دب", "🐻"), ("ذ", "ذال", "ذرة", "🌽"),
    ("ر", "راء", "رسالة", "📩"), ("ز", "زاي", "زرافة", "🦒"), ("س", "سين", "سمكة", "🐟"),
    ("ش", "شين", "شمس", "☀️"), ("ص", "صاد", "صقر", "🦅"), ("ض", "ضاد", "ضفدع", "🐸"),
    ("ط", "طاء", "طائرة", "✈️"), ("ظ", "ظاء", "ظبي", "🦌"), ("ع", "عين", "عنب", "🍇"),
    ("غ", "غين", "غيمة", "☁️"), ("ف", "فاء", "فيل", "🐘"), ("ق", "قاف", "قمر", "🌙"),
    ("ك", "كاف", "كتاب", "📖"), ("ل", "لام", "ليمون", "🍋"), ("م", "ميم", "موز", "🍌"),
    ("ن", "نون", "نحلة", "🐝"), ("ه", "هاء", "هدية", "🎁"), ("و", "واو", "وردة", "🌹"),
    ("ي", "ياء", "يد", "✋"),
]
DW = {"أرنب":"أَرْنَب","بطة":"بَطَّة","تفاحة":"تُفَّاحَة","ثعلب":"ثَعْلَب","جمل":"جَمَل","حصان":"حِصَان",
 "خروف":"خَرُوف","دب":"دُبّ","ذرة":"ذُرَة","رسالة":"رِسَالَة","زرافة":"زَرَافَة","سمكة":"سَمَكَة","شمس":"شَمْس",
 "صقر":"صَقْر","ضفدع":"ضِفْدَع","طائرة":"طَائِرَة","ظبي":"ظَبْي","عنب":"عِنَب","غيمة":"غَيْمَة","فيل":"فِيل",
 "قمر":"قَمَر","كتاب":"كِتَاب","ليمون":"لَيْمُون","موز":"مَوْز","نحلة":"نَحْلَة","هدية":"هَدِيَّة","وردة":"وَرْدَة","يد":"يَد"}
AL = {"ألف":"الأَلِف","باء":"البَاء","تاء":"التَّاء","ثاء":"الثَّاء","جيم":"الجِيم","حاء":"الحَاء","خاء":"الخَاء",
 "دال":"الدَّال","ذال":"الذَّال","راء":"الرَّاء","زاي":"الزَّاي","سين":"السِّين","شين":"الشِّين","صاد":"الصَّاد",
 "ضاد":"الضَّاد","طاء":"الطَّاء","ظاء":"الظَّاء","عين":"العَيْن","غين":"الغَيْن","فاء":"الفَاء","قاف":"القَاف",
 "كاف":"الكَاف","لام":"اللَّام","ميم":"المِيم","نون":"النُّون","هاء":"الهَاء","واو":"الوَاو","ياء":"اليَاء"}
# Pausal sukun on the last letter so the engine says "البَاءْ" not "البائي"
AL = {k: (v if v.endswith("\u0652") else v + "\u0652") for k, v in AL.items()}
SIMILAR = ["بتثني", "جحخ", "دذ", "رز", "سش", "صض", "طظ", "عغ", "فق", "كل", "مهوا"]
ALL_L = [l[0] for l in LETTERS]
BY_L = {l[0]: l for l in LETTERS}

def first_char(word):
    return "أ" if word[0] in "أا" else word[0]

def letter_distractors(letter, n=3):
    group = next((g for g in SIMILAR if letter in g), "")
    pool = [c for c in group if c != letter]
    rnd.shuffle(pool)
    out = pool[:n]
    rest = [c for c in ALL_L if c != letter and c not in out]
    rnd.shuffle(rest)
    out += rest[: n - len(out)]
    return out

def word_distractors(letter, n=3):
    others = [l for l in LETTERS if l[0] != letter]
    return rnd.sample(others, n)

def ex_find_letter(l):
    letter, name, _, _ = BY_L[l]
    return ex("choice", f"أين حرف ال{name}؟", [letter] + letter_distractors(letter), letter, speak=f"أَيْنَ حَرْفُ {AL[name]}؟")

def ex_listen_letter(l):
    letter, name, _, _ = BY_L[l]
    return ex("listen", "اسمع، ثم اختر الحرف", [letter] + letter_distractors(letter), letter,
              speak=f"حَرْفُ {AL[name]}")

def ex_word_starts(l):
    letter, name, word, emo = BY_L[l]
    correct = f"{emo} {word}"
    opts = [correct] + [f"{d[3]} {d[2]}" for d in word_distractors(letter)]
    return ex("choice", f"أي كلمة تبدأ بحرف {letter}؟", opts, correct,
              speak=f"أَيُّ كَلِمَةٍ تَبْدَأُ بِحَرْفِ {AL[name]}؟")

def ex_first_letter(l):
    letter, name, word, emo = BY_L[l]
    ans = first_char(word)
    opts = [ans] + letter_distractors(letter)
    return ex("choice", f"بأي حرف تبدأ كلمة {word}؟", opts, ans, show=emo,
              speak=f"بِأَيِّ حَرْفٍ تَبْدَأُ كَلِمَةُ {DW[word]}؟")

def arabic_level1():
    lessons, idx = [], 0
    for i, (letter, name, word, emo) in enumerate(LETTERS):
        idx += 1
        exs = [ex_find_letter(letter), ex_listen_letter(letter), ex_word_starts(letter), ex_first_letter(letter)]
        lessons.append(lesson(f"ar1_{idx:02d}", f"حرف ال{name}  {letter}", emo, letter,
                              f"هذا حرف ال{name}\n{emo} {word}",
                              f"هَذَا حَرْفُ {AL[name]}. {DW[word]}", exs))
        if (i + 1) % 4 == 0:
            idx += 1
            group = [l[0] for l in LETTERS[i - 3: i + 1]]
            makers = [ex_find_letter, ex_listen_letter, ex_first_letter]
            exs = [makers[k % 3](group[k % 4]) for k in range(6)]
            lessons.append(lesson(f"ar1_{idx:02d}", "مراجعة: " + " ".join(group), "🔁",
                                  " ".join(group), "لنراجع ما تعلّمناه",
                                  "هَيَّا نُرَاجِعُ الحُرُوفَ الَّتِي تَعَلَّمْنَاهَا", exs))
    return lessons

FATHA, DAMMA, KASRA, SUKUN = "َ", "ُ", "ِ", "ْ"
HARAKAT = [("الفتحة", FATHA, "فتحة"), ("الضمة", DAMMA, "ضمة"), ("الكسرة", KASRA, "كسرة")]
HSPK = {"فتحة": "فَتْحَة", "ضمة": "ضَمَّة", "كسرة": "كَسْرَة", "الفتحة": "الفَتْحَة", "الضمة": "الضَّمَّة", "الكسرة": "الكَسْرَة"}
HL = list("بتسمنلدركف")

def ex_haraka(letter, mark, mark_name):
    forms = [letter + FATHA, letter + DAMMA, letter + KASRA, letter + SUKUN]
    return ex("choice", f"أين الحرف الذي عليه {mark_name}؟", forms, letter + mark, speak=f"أَيْنَ الحَرْفُ الَّذِي عَلَيْهِ {HSPK[mark_name]}؟")

def arabic_level2():
    lessons, idx = [], 0
    for title, mark, mname in HARAKAT:
        idx += 1
        sample = rnd.sample(HL, 5)
        lessons.append(lesson(f"ar2_{idx:02d}", title, "✏️",
                              "  ".join(c + mark for c in sample[:3]),
                              f"هذه {title}، نضعها على الحرف فيتغيّر صوته",
                              f"هَذِهِ {HSPK[title]}. " + ". ".join(c + mark for c in sample[:3]),
                              [ex_haraka(c, mark, mname) for c in sample]))
    for k in range(2):
        idx += 1
        exs = []
        for c in rnd.sample(HL, 6):
            _, mark, mname = rnd.choice(HARAKAT)
            exs.append(ex_haraka(c, mark, mname))
        lessons.append(lesson(f"ar2_{idx:02d}", f"مراجعة الحركات {k + 1}", "🔁", FATHA + " " + DAMMA + " " + KASRA,
                              "فتحة، ضمة، كسرة", "هَيَّا نُرَاجِعُ الفَتْحَةَ وَالضَّمَّةَ وَالكَسْرَة", exs))
    chunks = [LETTERS[i::5] for i in range(5)]
    for k, chunk in enumerate(chunks):
        idx += 1
        exs = []
        for j, (letter, name, word, emo) in enumerate(chunk[:6]):
            others = rnd.sample([l for l in LETTERS if l[2] != word], 3)
            if j % 2 == 0:
                exs.append(ex("choice", "ما اسم هذه الصورة؟", [word] + [o[2] for o in others], word, show=emo, speak="مَا اسْمُ هَذِهِ الصُّورَة؟"))
            else:
                exs.append(ex("choice", "أي صورة لهذه الكلمة؟", [emo] + [o[3] for o in others], emo, show=word,
                              speak=f"أَيُّ صُورَةٍ لِكَلِمَةِ {DW[word]}؟"))
        lessons.append(lesson(f"ar2_{idx:02d}", f"كلمة وصورة {k + 1}", "🖼️", chunk[0][3],
                              "نقرأ الكلمة ونعرف صورتها", "هَيَّا نَقْرَأُ الكَلِمَاتِ وَنَعْرِفُ صُوَرَهَا", exs))
    for k, chunk in enumerate(chunks):
        idx += 1
        exs = []
        for (letter, name, word, emo) in chunk[:6]:
            ans = first_char(word)
            exs.append(ex("choice", "ما الحرف الناقص؟", [ans] + letter_distractors(letter), ans,
                          show=f"{emo}  ؟{word[1:]}", speak=f"مَا الحَرْفُ النَّاقِصُ فِي كَلِمَةِ {DW[word]}؟"))
        lessons.append(lesson(f"ar2_{idx:02d}", f"الحرف الناقص {k + 1}", "🧩", "؟",
                              "انظر إلى الصورة، واعرف الحرف الأول", "مَا الحَرْفُ النَّاقِص؟ اُنْظُرْ إِلَى الصُّورَة", exs))
    return lessons

# ---------------------------------------------------------------- Math
EMOJIS = ["🍎", "⭐", "🐟", "🌸", "🍋", "🐤", "⚽", "🍇"]

def num_opts(ans, lo, hi, n=4):
    pool = [x for x in range(max(lo, ans - 3), min(hi, ans + 3) + 1) if x != ans]
    rnd.shuffle(pool)
    out = pool[: n - 1]
    while len(out) < n - 1:
        x = rnd.randint(lo, hi)
        if x != ans and x not in out:
            out.append(x)
    return [str(ans)] + [str(x) for x in out]

def ex_count(n):
    e = rnd.choice(EMOJIS)
    return ex("count", "كم عددها؟", num_opts(n, 1, 10), str(n), count=n, count_emoji=e, speak="عُدَّ مَعِي. كَمْ عَدَدُهَا؟")

def ex_find_num(n, lo=0, hi=20):
    return ex("choice", f"أين العدد {n}؟", num_opts(n, lo, hi), str(n), speak=f"أَيْنَ العَدَدُ {n}؟")

def eq(s):
    return RLI + s.replace("?", "؟") + PDI

def math_level1():
    L, idx = [], 0
    for lo, hi in [(1, 3), (4, 6), (7, 9), (10, 10)]:
        idx += 1
        nums = list(range(lo, hi + 1))
        exs = [ex_count(rnd.choice(nums)) for _ in range(3)] + [ex_find_num(rnd.choice(nums), 1, 10) for _ in range(2)]
        L.append(lesson(f"m1_{idx:02d}", f"الأعداد {lo} - {hi}" if lo != hi else "العدد 10", "🔢",
                        "  ".join(str(x) for x in nums), "نتعلّم الأعداد ونعدّ الأشياء",
                        "هَيَّا نَتَعَلَّمُ الأَعْدَاد. " + ". ".join(str(x) for x in nums), exs))
    for k in range(2):
        idx += 1
        L.append(lesson(f"m1_{idx:02d}", f"نعدّ الأشياء {k + 1}", "🍎", "🍎🍎🍎", "عُدّ الأشياء واحداً واحداً",
                        "هَيَّا نَعُدُّ الأَشْيَاءَ مَعًا", [ex_count(rnd.randint(1, 10)) for _ in range(6)]))
    for lo, hi in [(11, 15), (16, 20)]:
        idx += 1
        exs = []
        for _ in range(3):
            n = rnd.randint(lo - 1, hi - 1)
            exs.append(ex("choice", f"ما العدد الذي بعد {n}؟", num_opts(n + 1, 1, 20), str(n + 1), speak=f"مَا العَدَدُ الَّذِي بَعْدَ {n}؟"))
        exs += [ex_find_num(rnd.randint(lo, hi), 10, 20) for _ in range(2)]
        L.append(lesson(f"m1_{idx:02d}", f"الأعداد {lo} - {hi}", "🔢", "  ".join(str(x) for x in range(lo, hi + 1)),
                        "نكمل العدّ حتى 20", "هَيَّا نُكْمِلُ العَدّ. " + ". ".join(str(x) for x in range(lo, hi + 1)), exs))
    SPW = {"أكبر": "أَكْبَر", "أصغر": "أَصْغَر"}
    for k, word in enumerate(["أكبر", "أصغر"]):
        idx += 1
        exs = []
        for _ in range(6):
            a, b = rnd.sample(range(1, 21), 2)
            ans = max(a, b) if word == "أكبر" else min(a, b)
            exs.append(ex("choice", f"أي عدد {word}؟", [str(a), str(b)], str(ans), speak=f"أَيُّ عَدَدٍ {SPW[word]}؟"))
        L.append(lesson(f"m1_{idx:02d}", f"العدد ال{word}", "⚖️", "5  ?  8", f"نعرف أي عدد هو ال{word}",
                        f"هَيَّا نَعْرِفُ العَدَدَ ال{SPW[word]}", exs))
    SHAPES = [("دائرة", "⚪"), ("مثلث", "🔺"), ("مربع", "⬛"), ("معيّن", "🔷"), ("نجمة", "⭐")]
    SAL = {"دائرة": "الدَّائِرَة", "مثلث": "المُثَلَّث", "مربع": "المُرَبَّع", "معيّن": "المُعَيَّن", "نجمة": "النَّجْمَة"}
    for k in range(2):
        idx += 1
        exs = []
        for name, emo in rnd.sample(SHAPES, 5):
            others = [s for s in SHAPES if s[0] != name]
            if k == 0:
                exs.append(ex("choice", "ما اسم هذا الشكل؟", [name] + [o[0] for o in rnd.sample(others, 3)], name, show=emo, speak="مَا اسْمُ هَذَا الشَّكْل؟"))
            else:
                exs.append(ex("choice", f"أين ال{name}؟" if name not in ("نجمة", "دائرة") else f"أين ال{name}؟",
                              [emo] + [o[1] for o in rnd.sample(others, 3)], emo, speak=f"أَيْنَ {SAL[name]}؟"))
        L.append(lesson(f"m1_{idx:02d}", f"الأشكال {k + 1}", "🔺", "⚪ 🔺 ⬛ 🔷", "دائرة، مثلث، مربع، معيّن",
                        "هَذِهِ دَائِرَة. هَذَا مُثَلَّث. هَذَا مُرَبَّع. هَذَا مُعَيَّن", exs))
    return L

def ex_op(a, b, op, hi):
    r = a + b if op == "+" else a - b
    return ex("choice", f"كم يساوي {a} {op} {b}؟", num_opts(r, 0, hi), str(r), show=eq(f"{a} {op} {b} = ?"),
              speak=f"كَمْ يُسَاوِي {a} {'زَائِد' if op == '+' else 'نَاقِص'} {b}؟")

def math_level2():
    L, idx = [], 0
    plan = [("الجمع حتى 10", "+", 10, 3, "➕"), ("الطرح حتى 10", "-", 10, 3, "➖"),
            ("الجمع حتى 20", "+", 20, 2, "➕"), ("الطرح حتى 20", "-", 20, 2, "➖")]
    for title, op, hi, count, icon in plan:
        for k in range(count):
            idx += 1
            exs = []
            for j in range(6):
                if op == "+":
                    a = rnd.randint(1 if hi == 10 else 5, hi - 1); b = rnd.randint(1, hi - a)
                else:
                    a = rnd.randint(2 if hi == 10 else 11, hi); b = rnd.randint(1, a - 1)
                if hi == 10 and k == 0 and j < 3:
                    e = rnd.choice(EMOJIS)
                    r = a + b if op == "+" else a - b
                    if op == "+":
                        exs.append(ex("choice", "كم المجموع؟", num_opts(r, 0, hi), str(r),
                                      show=eq(f"{e * a} + {e * b}"), speak="كَمِ المَجْمُوع؟"))
                    else:
                        exs.append(ex("choice", f"معك {a}، أخذنا {b}. كم بقي؟", num_opts(r, 0, hi), str(r),
                                      show=eq(f"{a} - {b} = ?"), speak=f"مَعَكَ {a}، أَخَذْنَا {b}. كَمْ بَقِيَ؟"))
                else:
                    exs.append(ex_op(a, b, op, hi))
            L.append(lesson(f"m2_{idx:02d}", f"{title} ({k + 1})", icon,
                            eq("3 + 2 = 5") if op == "+" else eq("5 - 2 = 3"),
                            "الجمع يعني نضيف" if op == "+" else "الطرح يعني نأخذ",
                            "ثَلَاثَة زَائِد اثْنَيْن يُسَاوِي خَمْسَة" if op == "+" else "خَمْسَة نَاقِص اثْنَيْن يُسَاوِي ثَلَاثَة", exs))
    for k in range(2):
        idx += 1
        exs = []
        for _ in range(6):
            t, o = rnd.randint(1, 9), rnd.randint(0, 9)
            n = t * 10 + o
            wrong = {o * 10 + t, t * 10 + ((o + 3) % 10), ((t % 9) + 1) * 10 + o} - {n}
            opts = [str(n)] + [str(w) for w in list(wrong)[:3]]
            while len(opts) < 4:
                w = str(rnd.randint(10, 99))
                if w not in opts: opts.append(w)
            exs.append(ex("choice", f"العدد الذي فيه {t} عشرات و{o} آحاد هو؟", opts, str(n), speak=f"مَا العَدَدُ الَّذِي فِيهِ {t} عَشَرَات وَ{o} آحَاد؟"))
        L.append(lesson(f"m2_{idx:02d}", f"الآحاد والعشرات {k + 1}", "🔟", "34 = 30 + 4",
                        "العدد 34 فيه 3 عشرات و4 آحاد", "أَرْبَعَةٌ وَثَلَاثُونَ فِيهَا ثَلَاثُ عَشَرَاتٍ وَأَرْبَعَةُ آحَاد", exs))
    for k, op in enumerate(["+", "-"]):
        idx += 1
        exs = []
        for _ in range(6):
            if op == "+":
                a = rnd.randint(1, 8) * 10; b = rnd.randint(1, (100 - a) // 10) * 10
            else:
                a = rnd.randint(2, 10) * 10; b = rnd.randint(1, a // 10 - 1) * 10
            r = a + b if op == "+" else a - b
            opts = [str(r)] + [str(x) for x in rnd.sample([v for v in range(0, 101, 10) if v != r], 3)]
            exs.append(ex("choice", f"كم يساوي {a} {op} {b}؟", opts, str(r), show=eq(f"{a} {op} {b} = ?"),
                          speak=f"كَمْ يُسَاوِي {a} {'زَائِد' if op == '+' else 'نَاقِص'} {b}؟"))
        L.append(lesson(f"m2_{idx:02d}", ("جمع" if op == "+" else "طرح") + " العشرات حتى 100", "💯",
                        eq("30 + 20 = 50") if op == "+" else eq("70 - 40 = 30"),
                        "نجمع ونطرح العشرات" , "هَيَّا نَحْسُبُ بِالعَشَرَات", exs))
    return L


# ---------------------------------------------------------------- English
EN = [("A","apple","🍎"),("B","ball","⚽"),("C","cat","🐱"),("D","dog","🐶"),("E","egg","🥚"),("F","fish","🐟"),
      ("G","grapes","🍇"),("H","hat","🎩"),("I","ice cream","🍦"),("J","jeans","👖"),("K","key","🔑"),("L","lion","🦁"),
      ("M","moon","🌙"),("N","nose","👃"),("O","orange","🍊"),("P","pen","🖊️"),("Q","queen","👸"),("R","rabbit","🐰"),
      ("S","sun","☀️"),("T","tree","🌳"),("U","umbrella","☂️"),("V","violin","🎻"),("W","watch","⌚"),("X","box","📦"),
      ("Y","yellow","💛"),("Z","zebra","🦓")]
EN_BY = {e[0]: e for e in EN}
EN_SIMILAR = ["BDPR", "CGOQ", "EFHT", "IJL", "MNWV", "KXYZ", "SUA"]

def en_distract(letter, n=3, lower=False):
    group = next((g for g in EN_SIMILAR if letter in g), "")
    pool = [c for c in group if c != letter]; rnd.shuffle(pool)
    out = pool[:n]
    rest = [e[0] for e in EN if e[0] != letter and e[0] not in out]; rnd.shuffle(rest)
    out += rest[: n - len(out)]
    return [c.lower() for c in out] if lower else out

def en_find(L):
    return ex("choice", f"أين حرف {L}؟", [L] + en_distract(L), L, speak=f"أَيْنَ حَرْف {L}؟")

def en_lower(L):
    return ex("choice", f"ما الحرف الصغير من {L}؟", [L.lower()] + en_distract(L, lower=True), L.lower(),
              show=L, speak=f"مَا الحَرْفُ الصَّغِيرُ مِنْ {L}؟")

def en_listen(L):
    return ex("listen", "اسمع، ثم اختر الحرف", [L] + en_distract(L), L, speak=f"The letter {L}.")

def en_word(L):
    _, w, e = EN_BY[L]
    others = rnd.sample([x for x in EN if x[0] != L], 3)
    if L == "X":
        return ex("choice", "أي كلمة فيها حرف X؟", [f"{e} {w}"] + [f"{o[2]} {o[1]}" for o in others], f"{e} {w}",
                  speak="أَيُّ كَلِمَةٍ فِيهَا حَرْف X؟")
    return ex("choice", f"أي كلمة تبدأ بحرف {L}؟", [f"{e} {w}"] + [f"{o[2]} {o[1]}" for o in others], f"{e} {w}",
              speak=f"أَيُّ كَلِمَةٍ تَبْدَأُ بِحَرْف {L}؟")

def english_level1():
    L, idx = [], 0
    pairs = [EN[i:i + 2] for i in range(0, 26, 2)]
    for k, pair in enumerate(pairs):
        idx += 1
        letters = [p[0] for p in pair]
        exs = []
        for c in letters:
            exs += [en_find(c), en_lower(c), en_word(c)]
        exs.append(en_listen(rnd.choice(letters)))
        L.append(lesson(f"en1_{idx:02d}", "الحروف " + " ".join(letters), pair[0][2],
                        "  ".join(f"{c}{c.lower()}" for c in letters),
                        "\n".join(f"{p[0]} {p[0].lower()} — {p[2]} {p[1]}" for p in pair),
                        " ".join(f"This is the letter {p[0]}. {p[0]}, {p[1]}." for p in pair), exs))
        if (k + 1) % 4 == 0 or k == len(pairs) - 1:
            idx += 1
            grp = [p[0] for pr in pairs[max(0, k - 3) if (k + 1) % 4 == 0 else (k // 4) * 4: k + 1] for p in pr]
            makers = [en_find, en_listen, en_lower, en_word]
            exs = [makers[j % 4](rnd.choice(grp)) for j in range(6)]
            L.append(lesson(f"en1_{idx:02d}", "مراجعة: " + " ".join(grp), "🔁", " ".join(grp),
                            "لنراجع الحروف", "هَيَّا نُرَاجِعُ الحُرُوف", exs))
    return L

EN_NUM = ["one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"]
EN_SETS = {
    "colors": ("الألوان", "🎨", [("red", "❤️"), ("blue", "💙"), ("green", "💚"), ("yellow", "💛"), ("purple", "💜"), ("black", "🖤")]),
    "animals1": ("الحيوانات 1", "🐶", [("cat", "🐱"), ("dog", "🐶"), ("cow", "🐮"), ("horse", "🐴"), ("bird", "🐦")]),
    "animals2": ("الحيوانات 2", "🦁", [("fish", "🐟"), ("duck", "🦆"), ("sheep", "🐑"), ("lion", "🦁"), ("monkey", "🐵")]),
    "food1": ("الطعام 1", "🍎", [("apple", "🍎"), ("banana", "🍌"), ("bread", "🍞"), ("milk", "🥛"), ("egg", "🥚")]),
    "food2": ("الطعام 2", "🍇", [("water", "💧"), ("orange", "🍊"), ("grapes", "🍇"), ("lemon", "🍋"), ("rice", "🍚")]),
    "body": ("الجسم", "👃", [("eye", "👁️"), ("nose", "👃"), ("ear", "👂"), ("hand", "✋"), ("mouth", "👄")]),
}
GREET = [("Hello", "👋", "مرحبا", "مَرْحَبًا"), ("Thank you", "🙏", "شكرًا", "شُكْرًا"), ("Good morning", "🌅", "صباح الخير", "صَبَاحُ الخَيْر"),
         ("Good night", "🌙", "تصبح على خير", "تُصْبِحُ عَلَى خَيْر"), ("Yes", "👍", "نعم", "نَعَم"), ("No", "👎", "لا", "لَا")]

def english_level2():
    L, idx = [], 0
    for k, (lo, hi) in enumerate([(1, 5), (6, 10)]):
        idx += 1
        nums = list(range(lo, hi + 1))
        exs = []
        for j in range(6):
            n = rnd.choice(nums)
            wrong = rnd.sample([w for w in EN_NUM if w != EN_NUM[n - 1]], 3)
            if j % 2 == 0:
                exs.append(ex("choice", "ما اسم هذا العدد بالإنجليزي؟", [EN_NUM[n - 1]] + wrong, EN_NUM[n - 1], show=str(n),
                              speak="مَا اسْمُ هَذَا العَدَدِ بِالإِنْجِلِيزِيّ؟"))
            else:
                e = rnd.choice(EMOJIS)
                exs.append(ex("count", "How many? كم عددها؟", [EN_NUM[n - 1]] + wrong, EN_NUM[n - 1], count=n, count_emoji=e,
                              speak="How many? كَمْ عَدَدُهَا؟"))
        L.append(lesson(f"en2_{idx:02d}", f"الأعداد بالإنجليزي {lo} - {hi}", "🔢", "  ".join(str(n) for n in nums),
                        "\n".join(f"{n} — {EN_NUM[n - 1]}" for n in nums),
                        ". ".join(EN_NUM[n - 1] for n in nums) + ".", exs))
    for key, (title, icon, items) in EN_SETS.items():
        idx += 1
        exs = []
        for j, (w, e) in enumerate(items + items[:1]):
            others = rnd.sample([x for x in items if x[0] != w], 3)
            if j % 3 == 0:
                exs.append(ex("choice", "ما اسمها بالإنجليزي؟", [w] + [o[0] for o in others], w, show=e,
                              speak="مَا اسْمُهَا بِالإِنْجِلِيزِيّ؟"))
            elif j % 3 == 1:
                exs.append(ex("listen", "اسمع، ثم اختر الصورة", [e] + [o[1] for o in others], e, speak=f"{w}."))
            else:
                exs.append(ex("choice", f"أين {w}؟", [e] + [o[1] for o in others], e, speak=f"Where is the {w}? أَيْنَ {w}؟"))
        L.append(lesson(f"en2_{idx:02d}", title, icon, " ".join(x[1] for x in items),
                        "\n".join(f"{x[1]} {x[0]}" for x in items), ". ".join(x[0] for x in items) + ".", exs))
    for k in range(2):
        idx += 1
        exs = []
        for en_, e, ar, ar_s in (GREET if k == 0 else GREET[::-1]):
            others = rnd.sample([g for g in GREET if g[0] != en_], 3)
            exs.append(ex("choice", f"كيف نقول «{ar}» بالإنجليزي؟", [en_] + [o[0] for o in others], en_, show=e,
                          speak=f"كَيْفَ نَقُولُ {ar_s} بِالإِنْجِلِيزِيّ؟"))
        L.append(lesson(f"en2_{idx:02d}", f"كلمات يومية {k + 1}", "👋", "👋 🙏 🌅",
                        "\n".join(f"{g[1]} {g[0]} = {g[2]}" for g in GREET),
                        " ".join(f"{g[0]}." for g in GREET), exs))
    return L

# ---------------------------------------------------------------- Placement
def placement():
    ar = [{"level": 1, "exercise": ex_find_letter(c)} for c in ["م", "ب", "س"]]
    ar += [{"level": 2, "exercise": ex("choice", "ما اسم هذه الصورة؟", ["فيل", "قمر", "شمس", "نحلة"], "فيل", show="🐘", speak="مَا اسْمُ هَذِهِ الصُّورَة؟")},
           {"level": 2, "exercise": ex("choice", "ما الحرف الناقص؟", ["ق", "ف", "غ", "ع"], "ق", show="🌙  ؟مر",
                                       speak="مَا الحَرْفُ النَّاقِصُ فِي كَلِمَةِ قَمَر؟")},
           {"level": 2, "exercise": ex_haraka("ب", DAMMA, "ضمة")}]
    m = [{"level": 1, "exercise": ex_count(n)} for n in [3, 5, 7]]
    m += [{"level": 2, "exercise": ex_op(a, b, op, 20)} for a, b, op in [(4, 3, "+"), (9, 5, "-"), (12, 5, "+")]]
    en = [{"level": 1, "exercise": en_find(c)} for c in ["B", "M", "S"]]
    en += [{"level": 2, "exercise": ex("choice", "ما اسمها بالإنجليزي؟", ["blue", "red", "green", "yellow"], "blue", show="💙",
                                       speak="مَا اسْمُهَا بِالإِنْجِلِيزِيّ؟")},
           {"level": 2, "exercise": ex("choice", "ما اسم هذا العدد بالإنجليزي؟", ["three", "five", "two", "eight"], "three", show="3",
                                       speak="مَا اسْمُ هَذَا العَدَدِ بِالإِنْجِلِيزِيّ؟")},
           {"level": 2, "exercise": ex("listen", "اسمع، ثم اختر الصورة", ["🐶", "🐱", "🐮", "🐦"], "🐶", speak="dog.")}]
    return {"ar": ar, "math": m, "en": en}

STORIES = [
    {"title": "الزيتونة الصغيرة", "icon": "🌱", "pages": [
        "كَانَتْ هُنَاكَ بِذْرَةُ زَيْتُونٍ صَغِيرَةٌ، نَائِمَةٌ تَحْتَ التُّرَاب.",
        "جَاءَ المَطَرُ، فَشَرِبَتِ البِذْرَةُ المَاءَ، وَقَالَتْ: شُكْرًا يَا مَطَر.",
        "ثُمَّ جَاءَتِ الشَّمْسُ، فَمَدَّتِ البِذْرَةُ وَرَقَةً خَضْرَاءَ صَغِيرَة.",
        "كُلَّ يَوْمٍ كَانَتْ تَكْبَرُ قَلِيلًا… قَلِيلًا فَقَط، لَكِنَّهَا لَمْ تَتَوَقَّف.",
        "وَصَارَتْ شَجَرَةَ زَيْتُونٍ جَمِيلَة. وَأَنْتَ أَيْضًا تَكْبَرُ وَتَتَعَلَّمُ كُلَّ يَوْمٍ قَلِيلًا 🌿"]},
    {"title": "العصفور ونجمة الليل", "icon": "🐦", "pages": [
        "فِي اللَّيْلِ، لَمْ يَسْتَطِعِ العُصْفُورُ الصَّغِيرُ أَنْ يَنَام.",
        "نَظَرَ إِلَى السَّمَاءِ، فَرَأَى نَجْمَةً تَلْمَعُ وَتَبْتَسِمُ لَه.",
        "قَالَتِ النَّجْمَةُ: أَغْمِضْ عَيْنَيْكَ، وَخُذْ نَفَسًا عَمِيقًا… ثُمَّ أَخْرِجْهُ بِبُطْء.",
        "فَعَلَ العُصْفُورُ ذَلِكَ ثَلَاثَ مَرَّاتٍ، فَشَعَرَ بِالهُدُوء.",
        "وَنَامَ العُصْفُورُ، وَفِي الصَّبَاحِ غَنَّى أَجْمَلَ أُغْنِيَة ☀️"]},
    {"title": "سمكة البحر الملوّنة", "icon": "🐠", "pages": [
        "فِي البَحْرِ الأَزْرَقِ، عَاشَتْ سَمَكَةٌ صَغِيرَةٌ مُلَوَّنَة.",
        "رَأَتْ سَمَكَةً أُخْرَى وَحِيدَةً، فَسَبَحَتْ إِلَيْهَا وَقَالَتْ: هَلْ نَلْعَبُ مَعًا؟",
        "لَعِبَتِ السَّمَكَتَانِ، وَضَحِكَتَا، وَسَبَحَتَا بَيْنَ الأَمْوَاج.",
        "وَعَرَفَتِ السَّمَكَةُ الصَّغِيرَةُ أَنَّ الصَّدِيقَ يَجْعَلُ اليَوْمَ أَجْمَل.",
        "هَلْ عِنْدَكَ صَدِيق؟ قُلْ لَهُ كَلِمَةً حُلْوَةً اليَوْم 💛"]},
    {"title": "شمس الصباح", "icon": "☀️", "pages": [
        "اِسْتَيْقَظَتِ الشَّمْسُ، وَمَدَّتْ أَشِعَّتَهَا الدَّافِئَة.",
        "قَالَتْ لِلْأَطْفَالِ: صَبَاحَ الخَيْر! اليَوْمُ يَوْمٌ جَدِيد.",
        "اليَوْمَ نَتَعَلَّمُ حَرْفًا جَدِيدًا، أَوْ عَدَدًا جَدِيدًا.",
        "وَإِذَا أَخْطَأْنَا، نُحَاوِلُ مَرَّةً أُخْرَى، فَالمُحَاوَلَةُ شَجَاعَة.",
        "اِبْتَسَمَتِ الشَّمْسُ وَقَالَتْ: أَنَا فَخُورَةٌ بِك 🌟"]},
]

# Fixed phrases the app speaks (all get a recorded clip).
PHRASES = {
    "praise": "أَحْسَنْت",
    "retry": "حَاوِلْ مَرَّةً أُخْرَى",
    "welcome": "أَهْلًا وَسَهْلًا فِي مَدْرَسَةِ غَزَّةَ الصَّغِيرَة. مَنْ يُرِيدُ أَنْ يَتَعَلَّمَ اليَوْم؟",
    "hello": "أَهْلًا وَسَهْلًا! هَيَّا نَتَعَلَّم",
    "lesson_done_3": "مُمْتَاز! أَنْتَ بَطَل",
    "lesson_done": "أَحْسَنْت! أَنْهَيْتَ الدَّرْس",
    "placement_done": "رَائِع! جَهَّزْنَا لَكَ الدُّرُوسَ المُنَاسِبَة",
    "placement_start_ar": "الآنَ اللُّغَةُ العَرَبِيَّة. أَجِبْ عَلَى قَدْرِ مَا تَعْرِف، وَلَا بَأْسَ إِنْ لَمْ تَعْرِف",
    "placement_start_math": "الآنَ الرِّيَاضِيَّات. أَجِبْ عَلَى قَدْرِ مَا تَعْرِف، وَلَا بَأْسَ إِنْ لَمْ تَعْرِف",
    "placement_start_en": "الآنَ اللُّغَةُ الإِنْجِلِيزِيَّة. أَجِبْ عَلَى قَدْرِ مَا تَعْرِف، وَلَا بَأْسَ إِنْ لَمْ تَعْرِف",
    "calm": "رُكْنُ الهُدُوء. هُنَا نَرْتَاحُ قَلِيلًا",
    "breathe_in": "خُذْ نَفَسًا عَمِيقًا",
    "breathe_out": "أَخْرِجِ النَّفَسَ بِبُطْء",
    "breathe_done": "أَحْسَنْت. أَنْتَ الآنَ أَهْدَأ",
    "feelings": "كَيْفَ تَشْعُرُ اليَوْم؟",
    "feel_0": "رَائِع! الفَرَحُ جَمِيل، شَارِكْهُ مَعَ مَنْ تُحِبّ.",
    "feel_1": "لَا بَأْسَ أَنْ تَحْزَن. خُذْ نَفَسًا عَمِيقًا، وَاحْكِ لِشَخْصٍ كَبِيرٍ تَثِقُ بِه.",
    "feel_2": "الغَضَبُ شُعُورٌ طَبِيعِيّ. لِنَعُدَّ مَعًا بِبُطْءٍ مِنْ وَاحِدٍ إِلَى خَمْسَة، ثُمَّ نَتَنَفَّس.",
    "feel_3": "أَنْتَ لَسْتَ وَحْدَك. اِقْتَرِبْ مِنْ شَخْصٍ كَبِيرٍ تُحِبُّه، وَجَرِّبْ تَمْرِينَ التَّنَفُّس.",
    "feel_4": "جِسْمُكَ يَحْتَاجُ إِلَى رَاحَة. اِشْرَبْ قَلِيلًا مِنَ المَاء، وَاسْتَرِحْ قَلِيلًا.",
    "test": "مَرْحَبًا! هَذَا حَرْفُ البَاءْ. بَطَّة",
}

def main(out):
    subjects = [
        {"id": "ar", "title": "اللغة العربية", "speakTitle": "اللُّغَةُ العَرَبِيَّة", "icon": "📖", "levels": [
            {"level": 1, "title": "المستوى الأول: الحروف", "lessons": arabic_level1()},
            {"level": 2, "title": "المستوى الثاني: الحركات والكلمات", "lessons": arabic_level2()}]},
        {"id": "math", "title": "الرياضيات", "speakTitle": "الرِّيَاضِيَّات", "icon": "🔢", "levels": [
            {"level": 1, "title": "المستوى الأول: الأعداد والأشكال", "lessons": math_level1()},
            {"level": 2, "title": "المستوى الثاني: الجمع والطرح", "lessons": math_level2()}]},
        {"id": "en", "title": "اللغة الإنجليزية", "speakTitle": "اللُّغَةُ الإِنْجِلِيزِيَّة", "icon": "🔤", "levels": [
            {"level": 1, "title": "المستوى الأول: الحروف A - Z", "lessons": english_level1()},
            {"level": 2, "title": "المستوى الثاني: كلمات وأعداد وألوان", "lessons": english_level2()}]},
    ]
    data = {"version": 1, "subjects": subjects, "placement": placement(), "stories": STORIES, "phrases": PHRASES}
    with open(out, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
    total = sum(len(lv["lessons"]) for s in subjects for lv in s["levels"])
    exs = sum(len(l["exercises"]) for s in subjects for lv in s["levels"] for l in lv["lessons"])
    print("lessons", total, "exercises", exs)

if __name__ == "__main__":
    main(sys.argv[1])
