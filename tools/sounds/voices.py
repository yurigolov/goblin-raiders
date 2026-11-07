"""Goblin voices from the user's own recordings (tools/sounds/recordings), written as mono Ogg Vorbis into
assets/goblinraiders/sounds/<file>_<n>.ogg, one file per take.

A session is a recording plus a JSON map: section -> takes as [start, end] seconds (see recordings/session_1.json and
recordings/README.md for the sections). Each take is cut with a little air around it, its rumble and hum filtered out,
its ends faded, its level evened out; then it gets the goblin's voice: shifted up or down by resampling (which moves
the pitch and the size of the head together, like the game's own pitch), a touch of throat grit, a room for the shaman
and the king. The game varies the pitch further per goblin (pitchFactor in each entity class), so a section is shifted
here only by what the recording lacks.

Sections nobody has recorded yet are made, for now, from the takes of similar ones (FALLBACKS).

Run with a Python that has numpy, scipy and soundfile:  python tools/sounds/voices.py
Every section in a row, announced, goes to tools/previews/voice_review.ogg when the Windows Russian voice is there
(the index of where each starts is printed).
"""
import json
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from fractions import Fraction
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy import signal

ROOT = Path(__file__).resolve().parents[2]
RECORDINGS = ROOT / 'tools/sounds/recordings'
OUT = ROOT / 'src/main/resources/assets/goblinraiders/sounds'
REVIEW = ROOT / 'tools/previews/voice_review.ogg'
RATE = 32000
rng = np.random.default_rng(7)


@dataclass(frozen=True)
class Voice:
    file: str             # output file prefix (the sound event lists <file>_1.ogg, <file>_2.ogg ...)
    label: str            # how the review names it
    shift: float = 0.0    # semitones
    grit: float = 0.0     # throat grit, 0..1
    room: float = 0.0     # reverb, 0..1
    dark: float = 0.0     # a darker tone, 0..1 (big throats)
    game_pitch: float = 1.0   # what the game multiplies it by (pitchFactor), for the review only


# Section -> voice. game_pitch is the pitchFactor of the goblin that mostly plays it.
VOICES = {
    'goblin_ambient': Voice('goblin_ambient', 'Гоблин бормочет', 3, 0.3),
    'goblin_talk': Voice('goblin_talk', 'Гоблин говорит', 3, 0.3),
    'goblin_laugh': Voice('goblin_laugh', 'Гоблин смеётся', 3, 0.3),
    'goblin_angry': Voice('goblin_angry', 'Гоблин злится', 3, 0.35),
    'goblin_hurt': Voice('goblin_hurt', 'Гоблину больно', 3, 0.3),
    'goblin_death': Voice('goblin_death', 'Гоблин умирает', 3, 0.3),
    'goblin_attack': Voice('grunt', 'Удар', 3, 0.35),
    'goblin_gloat': Voice('gloat', 'Злорадство', 3, 0.3),
    'goblin_scared': Voice('squeal', 'Испуг', 2, 0.1),
    'goblin_yes': Voice('goblin_yes', 'Да', 3, 0.25),
    'goblin_no': Voice('goblin_no', 'Нет', 3, 0.25),
    'child_giggle': Voice('giggle', 'Гоблинёнок хихикает', 0, 0.0, game_pitch=1.35),
    'giant_ambient': Voice('giant_ambient', 'Огромный ворчит', -2, 0.35, dark=0.5, game_pitch=0.8),
    'giant_roar': Voice('giant_roar', 'Огромный ревёт', -2, 0.5, 0.15, 0.5, 0.8),
    'giant_hurt': Voice('giant_hurt', 'Огромному больно', -2, 0.35, dark=0.5, game_pitch=0.8),
    'giant_death': Voice('giant_death', 'Огромный умирает', -2, 0.35, 0.1, 0.5, 0.8),
    'giant_attack': Voice('giant_attack', 'Огромный бьёт', -2, 0.45, dark=0.5, game_pitch=0.8),
    'trader_haggle': Voice('trader_haggle', 'Торгаш торгуется', 1, 0.15, game_pitch=1.2),
    'trader_yell': Voice('trader_yell', 'Торгаш орёт', 1, 0.3, game_pitch=1.2),
    'elder_ambient': Voice('elder_ambient', 'Старец вздыхает', 2, 0.0, game_pitch=0.85),
    'elder_talk': Voice('elder_talk', 'Старец говорит', 2, 0.0, game_pitch=0.85),
    'crazy_laugh': Voice('crazy_laugh', 'Сумасшедший хохочет', 1, 0.2, game_pitch=1.15),
    'shaman_ambient': Voice('shaman_ambient', 'Шаман бормочет', 2, 0.1, 0.3, game_pitch=1.05),
    'shaman_spell': Voice('shaman_spell', 'Шаман колдует', 2, 0.15, 0.45, game_pitch=1.05),
    'king_ambient': Voice('king_ambient', 'Король ворчит', 0, 0.3, 0.2, 0.4, 0.75),
    'king_roar': Voice('king_roar', 'Король ревёт', 0, 0.5, 0.35, 0.4, 0.75),
    'ruthless_ambient': Voice('ruthless_ambient', 'Безжалостный рычит', 1, 0.4, game_pitch=0.9),
    'ruthless_hurt': Voice('ruthless_hurt', 'Безжалостному больно', 1, 0.4, game_pitch=0.9),
    'ruthless_death': Voice('ruthless_death', 'Безжалостный умирает', 1, 0.4, game_pitch=0.9),
}

# Until the most needed sections are recorded: (section, extra semitones to bring it to an ordinary goblin).
FALLBACKS = {
    'goblin_ambient': [('goblin_yes', 0), ('goblin_no', 0), ('elder_ambient', 1)],
    'goblin_talk': [('trader_haggle', 2), ('elder_talk', 1)],
    'goblin_laugh': [('goblin_gloat', 0), ('crazy_laugh', -1)],
    'goblin_angry': [('trader_yell', 2), ('giant_attack', 5)],
    'goblin_hurt': [('giant_hurt', 5), ('ruthless_hurt', 2)],
    'goblin_death': [('giant_death', 5), ('ruthless_death', 2)],
}


# ---------------------------------------------------------------- the takes
def load_sessions():
    """{section: [(samples at RATE)]} from every session in recordings/."""
    takes = {}
    for meta in sorted(RECORDINGS.glob('session_*.json')):
        session = json.loads(meta.read_text(encoding='utf-8'))
        audio, rate = sf.read(RECORDINGS / session['source'], dtype='float64', always_2d=True)
        audio = audio.mean(axis=1)
        for section, spans in session['takes'].items():
            for start, end in spans:
                a, b = int((start - 0.06) * rate), int((end + 0.2) * rate)
                take = audio[max(0, a):min(len(audio), b)]
                takes.setdefault(section, []).append(resample(take, rate, RATE))
    return takes


def resample(x, rate_from, rate_to):
    ratio = Fraction(rate_to, rate_from).limit_denominator(1000)
    return signal.resample_poly(x, ratio.numerator, ratio.denominator)


def clean(x):
    """No rumble or hum, the silence around the voice trimmed to a breath, soft ends, an even level."""
    x = signal.sosfilt(signal.butter(4, 85, 'highpass', fs=RATE, output='sos'), x)
    env = np.abs(signal.hilbert(x))
    env = signal.sosfiltfilt(signal.butter(2, 30, 'lowpass', fs=RATE, output='sos'), env)
    loud = np.nonzero(env > env.max() * 0.03)[0]
    if len(loud):
        x = x[max(0, loud[0] - int(0.03 * RATE)):min(len(x), loud[-1] + int(0.12 * RATE))]
    x = fade(x, 0.008, 0.06)
    voiced = x[np.abs(x) > np.abs(x).max() * 0.1]
    rms = np.sqrt(np.mean(voiced ** 2)) if len(voiced) else 1e-3
    return x * (0.18 / rms)


def fade(x, fade_in, fade_out):
    x = x.copy()
    n_in, n_out = min(len(x), int(fade_in * RATE)), min(len(x), int(fade_out * RATE))
    x[:n_in] *= np.linspace(0, 1, n_in) ** 2
    x[len(x) - n_out:] *= np.linspace(1, 0, n_out) ** 2
    return x


# ---------------------------------------------------------------- the goblin voice
def shift(x, semitones):
    """Pitch and formants together, by resampling (the take gets shorter going up)."""
    if abs(semitones) < 0.01:
        return x
    ratio = Fraction(2 ** (semitones / 12)).limit_denominator(300)
    return signal.resample_poly(x, ratio.denominator, ratio.numerator)


def grit(x, amount):
    """Throat grit: a soft saturation mixed in, its brittle top filtered off."""
    if amount <= 0:
        return x
    drive = 1.0 + amount * 6.0
    peak = np.abs(x).max() + 1e-9
    crunched = np.tanh(x / peak * drive) / np.tanh(drive) * peak
    crunched = signal.sosfilt(signal.butter(2, 5500, 'lowpass', fs=RATE, output='sos'), crunched)
    return x * (1.0 - amount) + crunched * amount


def darken(x, amount):
    if amount <= 0:
        return x
    low = signal.sosfilt(signal.butter(2, 3200, 'lowpass', fs=RATE, output='sos'), x)
    return x * (1.0 - amount) + low * amount


def room(x, amount, seconds=1.2):
    """A stone hall: the dry voice and its convolution with a decaying, darkened noise tail."""
    if amount <= 0:
        return x
    t = np.arange(int(seconds * RATE)) / RATE
    tail = rng.standard_normal(len(t)) * np.exp(-t * 6.0 / seconds)
    tail = signal.sosfilt(signal.butter(2, 3000, 'lowpass', fs=RATE, output='sos'), tail)
    tail[:int(0.012 * RATE)] = 0.0                    # the first reflection comes a little later
    tail /= np.sqrt(np.sum(tail ** 2))
    wet = signal.fftconvolve(x, tail)
    dry = np.concatenate([x, np.zeros(len(wet) - len(x))])
    return dry + wet * amount * 3.0


def finish(x, peak=0.8):
    """A soft limiter, and headroom: Vorbis lifts the peaks a little."""
    x = x / (np.abs(x).max() + 1e-9)
    x = np.tanh(x * 1.6) / np.tanh(1.6)
    return fade(x * peak, 0.003, 0.03)


def voice(take, v, extra=0.0):
    x = shift(clean(take), v.shift + extra)
    x = darken(grit(x, v.grit), v.dark)
    return finish(room(x, v.room))


def chatter(talk, laugh, count=3):
    """Goblins talking somewhere around the corner: two or three lines overlapping, quieter, duller, in a room."""
    out = []
    for k in range(count):
        mix = np.zeros(int(0.2 * RATE))
        at = 0.0
        for j in range(2 + k % 2):
            line = talk[(k * 3 + j) % len(talk)]
            line = shift(line, rng.uniform(-1.5, 2.0)) * rng.uniform(0.6, 1.0)
            start = int(at * RATE)
            if start + len(line) > len(mix):
                mix = np.concatenate([mix, np.zeros(start + len(line) - len(mix))])
            mix[start:start + len(line)] += line
            at += len(line) / RATE * rng.uniform(0.55, 0.85)
        if laugh and k != 1:
            cackle = laugh[k % len(laugh)] * 0.6
            start = int(at * RATE)
            mix = np.concatenate([mix, np.zeros(max(0, start + len(cackle) - len(mix)))])
            mix[start:start + len(cackle)] += cackle
        dull = signal.sosfilt(signal.butter(2, 2500, 'lowpass', fs=RATE, output='sos'), mix)
        out.append(finish(room(dull, 0.35), 0.8))
    return out


# ---------------------------------------------------------------- output
def write(path, x):
    """Ogg Vorbis in one-second pieces: libsndfile overflows its stack encoding a long buffer in one call."""
    with sf.SoundFile(path, 'w', RATE, 1, format='OGG', subtype='VORBIS') as out:
        for start in range(0, len(x), RATE):
            out.write(x[start:start + RATE].astype(np.float32))


def save(prefix, sounds):
    for old in OUT.glob(f'{prefix}_*.ogg'):
        if old.stem[len(prefix) + 1:].isdigit():
            old.unlink()
    for n, x in enumerate(sounds, 1):
        write(OUT / f'{prefix}_{n}.ogg', x)


def build():
    takes = load_sessions()
    made = {}
    for section, v in VOICES.items():
        if section in takes:
            made[section] = (v, [voice(t, v) for t in takes[section]], False)
        else:
            sounds = [voice(t, VOICES[source], extra) for source, extra in FALLBACKS.get(section, []) for t in takes.get(source, [])]
            if sounds:
                made[section] = (v, sounds, True)
    talk = [clean(t) for t in takes.get('goblin_talk', takes.get('trader_haggle', []) + takes.get('elder_talk', []))]
    talk = [shift(t, 3) for t in talk]
    laugh = [shift(clean(t), 3) for t in takes.get('goblin_laugh', takes.get('goblin_gloat', []))]
    if talk:
        made['chatter'] = (Voice('chatter', 'Гомон в деревне'), chatter(talk, laugh), 'goblin_talk' not in takes)
    for v, sounds, _ in made.values():
        save(v.file, sounds)
    return made


def review(made):
    """Every section in a row, announced by the Windows Russian voice, pitched as the game will play it."""
    labels = {section: (v.label + (', временно' if borrowed else '')) for section, (v, _, borrowed) in made.items()}
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        script = tmp / 'say.ps1'
        lines = tmp / 'lines.txt'
        lines.write_text('\n'.join(f'{k}|{t}' for k, t in labels.items()) + '\n', encoding='utf-8')
        script.write_text('''param([string]$Out, [string]$List)
Add-Type -AssemblyName System.Speech
$f = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(32000, [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen, [System.Speech.AudioFormat.AudioChannel]::Mono)
$s = New-Object System.Speech.Synthesis.SpeechSynthesizer
$s.SelectVoice('Microsoft Irina Desktop'); $s.Rate = 2
foreach ($l in Get-Content -Encoding UTF8 $List) { $p = $l.Split('|'); $s.SetOutputToWaveFile((Join-Path $Out ($p[0] + '.wav')), $f); $s.Speak($p[1]) }
$s.SetOutputToNull(); $s.Dispose()
''', encoding='utf-8')
        try:
            subprocess.run(['powershell.exe', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', str(script), '-Out', str(tmp),
                            '-List', str(lines)], check=True, capture_output=True)
        except (OSError, subprocess.CalledProcessError):
            print('no Windows voice: no review')
            return
        reel, at, index = [np.zeros(int(0.3 * RATE))], 0.3, []
        for section, (v, sounds, borrowed) in made.items():
            name, _ = sf.read(tmp / f'{section}.wav', dtype='float64')
            name = name / (np.abs(name).max() + 1e-9) * 0.4
            minutes, tenths = divmod(round(at * 10), 600)
            index.append(f'{minutes}:{tenths / 10:04.1f}  {labels[section]} ({len(sounds)})')
            reel += [name, np.zeros(int(0.35 * RATE))]
            at += len(name) / RATE + 0.35
            game = 12 * np.log2(v.game_pitch)
            for x in sounds:
                y = shift(x, game) * 0.85
                reel += [y, np.zeros(int(0.45 * RATE))]
                at += len(y) / RATE + 0.45
            reel.append(np.zeros(int(0.5 * RATE)))
            at += 0.5
        write(REVIEW, np.concatenate(reel))
        REVIEW.with_suffix('.txt').write_text('\n'.join(index) + '\n', encoding='utf-8')
        print('\n'.join(index))


if __name__ == '__main__':
    made = build()
    print(f'{sum(len(s) for _, s, _ in made.values())} voice files in {len(made)} sections')
    if '--no-review' not in sys.argv:
        review(made)
