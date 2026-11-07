"""Synthesized sounds of the goblin settlements, written as mono Ogg Vorbis into assets/goblinraiders/sounds/.
Nothing here is borrowed from the game: every sound is drawn from oscillators, filtered noise and envelopes.

    war_drums_1..3   war drums of the war camps: big low drums, a smaller drum and rim clicks, in a hall-like echo
    war_horn_1..2    a war horn: a brassy call rising a fifth, or one long wailing note, echoing over the camp
    forge_1..3       a smith at work: dull blows on the hot iron and bright taps on the anvil between them
    sharpen_1..3     a blade drawn over a whetstone, ringing at the end of each stroke
    chop_1..3        an axe biting into a log, the last blow splitting it
    farm_1..3        a hoe in the soil: chops and a drag through crumbling earth
    coins_1..3       coins: poured on a pile, counted one by one, a purse shaken
    jingle_1..3      a loot sack on the move: coins and trinkets clinking at every step
    bell_1..2        the hamlet's bell, rung on the square in the morning
    chimes_1..3      wind chimes and charms by the sage's tent
    pipe_tune_1..4   a settler's pipe: short pentatonic tunes, breathy, with a little vibrato
    crickets_1..2    crickets at night: a few insects chirping at their own pitch and pace
    owl_1..2         an owl's hoots
    birds_1..3       songbirds: whistles, sweeps and trills
    snore_1..3       goblin snores: a rattling breath in, then a whistle, a lip flutter or a snort out

The goblins' voices are not made here: they are the user's recordings, processed by tools/sounds/voices.py.

Run with a Python that has numpy, scipy and soundfile:  python tools/sounds/synth.py
Waveforms of all of them go to tools/previews/sounds.png, and every sound in a row, with pauses, to
tools/previews/sound_demo.ogg (the order is printed).
"""
import zlib
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy import signal

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'src/main/resources/assets/goblinraiders/sounds'
PREVIEW = ROOT / 'tools/previews/sounds.png'
DEMO = ROOT / 'tools/previews/sound_demo.ogg'
RATE = 32000
rng = np.random.default_rng(0)


def t_axis(seconds):
    return np.arange(int(seconds * RATE)) / RATE


def silence(seconds):
    return np.zeros(int(seconds * RATE))


def band(x, low, high, order=2):
    high = min(high, RATE / 2 - 200)
    sos = signal.butter(order, [low, high], btype='bandpass', fs=RATE, output='sos')
    return signal.sosfilt(sos, x)


def lowpass(x, cutoff, order=2):
    return signal.sosfilt(signal.butter(order, min(cutoff, RATE / 2 - 200), btype='lowpass', fs=RATE, output='sos'), x)


def highpass(x, cutoff, order=2):
    return signal.sosfilt(signal.butter(order, cutoff, btype='highpass', fs=RATE, output='sos'), x)


def noise(seconds):
    return rng.standard_normal(int(seconds * RATE))


def place(track, sound, at):
    """Mixes `sound` into `track` starting at `at` seconds (the track grows if needed)."""
    start = max(0, int(at * RATE))
    end = start + len(sound)
    if end > len(track):
        track = np.concatenate([track, np.zeros(end - len(track))])
    track[start:end] += sound
    return track


def fade(x, fade_in=0.01, fade_out=0.05):
    x = x.copy()
    n_in, n_out = min(len(x), int(fade_in * RATE)), min(len(x), int(fade_out * RATE))
    if n_in:
        x[:n_in] *= np.linspace(0, 1, n_in)
    if n_out:
        x[-n_out:] *= np.linspace(1, 0, n_out)
    return x


def room(x, seconds=0.9, wet=0.25, brightness=2500):
    """A plain reverb: the dry sound plus its convolution with a decaying, darkened noise tail."""
    tail = rng.standard_normal(int(seconds * RATE)) * np.exp(-t_axis(seconds) * 6.0 / seconds)
    tail = lowpass(tail, brightness)
    tail /= np.sqrt(np.sum(tail ** 2))
    dry = np.concatenate([x, np.zeros(len(tail))])
    wet_part = np.zeros(len(dry))
    convolved = signal.fftconvolve(x, tail)
    wet_part[:len(convolved)] = convolved
    return dry + wet * wet_part * 8.0


def normalize(x, peak=0.89):
    return x * (peak / max(1e-9, np.abs(x).max()))


def finish(x, peak=0.85, tail=0.3):
    """Pads a short tail of silence, fades the ends and normalizes."""
    return normalize(fade(np.concatenate([x, silence(tail)]), 0.002, 0.15), peak)


# ---------------------------------------------------------------- metal: struck partials
def ring(freqs, amps, decays, length, strike=0.3, strike_band=(2000, 9000), strike_decay=300.0):
    """A struck metal body: decaying sines at its (inharmonic) partials and the click of the blow."""
    t = t_axis(length)
    out = np.zeros(len(t))
    for f, a, d in zip(freqs, amps, decays):
        if f < RATE / 2 - 500:
            out += a * np.sin(2 * np.pi * f * t + rng.random() * 6.283) * np.exp(-t / d)
    out += band(rng.standard_normal(len(t)), *strike_band) * np.exp(-t * strike_decay) * strike
    return fade(out, 0.0003, 0.05)


def anvil_tap(level=1.0):
    f = 1150 * (0.95 + 0.1 * rng.random())
    ratios = [1.0, 1.58, 2.31, 2.97, 3.89, 4.62]
    return ring([f * r for r in ratios], [1.0, 0.6, 0.5, 0.35, 0.25, 0.15], [0.7, 0.5, 0.4, 0.3, 0.22, 0.16], 1.0, 0.25) * level


def hot_iron_blow(level=1.0):
    """A blow on the glowing work piece: duller, a short ring and the thud of the hammer."""
    f = 640 * (0.95 + 0.1 * rng.random())
    metal = ring([f, f * 1.71, f * 2.43, f * 3.6], [1.0, 0.5, 0.3, 0.2], [0.12, 0.08, 0.06, 0.04], 0.5, 0.5, (800, 5000), 120.0)
    t = t_axis(0.5)
    thud = np.sin(2 * np.pi * (150 + 80 * np.exp(-t * 40)) * t) * np.exp(-t / 0.05) * 0.8
    return (metal + thud) * level


def forge(pattern):
    """pattern: (time, 'B' blow on the iron or 't' tap on the anvil, level)."""
    track = silence(0.1)
    for at, kind, level in pattern:
        track = place(track, (hot_iron_blow if kind == 'B' else anvil_tap)(level), at)
    return finish(room(track, 0.8, 0.15, 5000))


def coin(level=1.0):
    f = 3600 * (0.85 + 0.3 * rng.random())
    return ring([f, f * 1.53, f * 2.12, f * 2.67], [1.0, 0.7, 0.5, 0.3],
                [0.12 + 0.1 * rng.random(), 0.08, 0.06, 0.04], 0.4, 0.15, (5000, 12000), 400.0) * level


def coin_drop(at, level=1.0):
    """One coin landing on others: a hit and a few quick bounces."""
    hits = []
    gap, lv = 0.05 + 0.03 * rng.random(), level
    for _ in range(2 + rng.integers(0, 3)):
        hits.append((at, lv))
        at += gap
        gap *= 0.6
        lv *= 0.5
    return hits


def coins(kind):
    track = silence(0.05)
    if kind == 'pour':
        hits = []
        at = 0.05
        for k in range(9):
            hits += coin_drop(at, 0.6 + 0.4 * rng.random())
            at += 0.04 + 0.08 * rng.random()
    elif kind == 'count':
        hits = []
        for k, at in enumerate([0.05, 0.45, 0.8, 1.2, 1.55]):
            hits += coin_drop(at, 0.8)
    else:   # a purse shaken: many little clinks and the rustle of leather
        hits = [(0.05 + 0.6 * rng.random(), 0.25 + 0.3 * rng.random()) for _ in range(26)]
        t = t_axis(0.7)
        rustle = lowpass(rng.standard_normal(len(t)), 2500) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t)) * np.sin(np.pi * t / 0.7) * 0.25
        track = place(track, rustle, 0.03)
    for at, level in hits:
        track = place(track, coin(level), at)
    return finish(room(track, 0.4, 0.1, 9000), 0.8)


def jingle():
    """Three steps of a loot sack: coins and small trinkets shaking together, a little chain."""
    track = silence(0.05)
    for step in range(3):
        at = 0.05 + step * 0.42 + rng.normal(0, 0.02)
        for _ in range(3 + rng.integers(0, 3)):
            track = place(track, coin(0.3 + 0.4 * rng.random()), at + rng.random() * 0.08)
        for _ in range(6):   # chain links: very short, high ticks
            t = t_axis(0.03)
            tick = band(rng.standard_normal(len(t)), 5000, 11000) * np.exp(-t * 200) * 0.3
            track = place(track, tick, at + rng.random() * 0.12)
    return finish(track, 0.75)


def bell(rings):
    """A village bell: hum, prime, minor third, fifth, nominal and the bright partials, each a slightly beating pair."""
    f = 330.0
    parts = [(0.5, 0.5, 4.0), (1.0, 1.0, 3.2), (1.19, 0.6, 2.4), (1.5, 0.45, 2.0), (2.0, 0.7, 1.6), (2.5, 0.3, 1.1),
             (2.67, 0.35, 0.9), (3.0, 0.25, 0.8), (4.0, 0.2, 0.5)]
    track = silence(0.05)
    for at, level in rings:
        freqs, amps, decays = [], [], []
        for ratio, amp, decay in parts:
            for detune in (-0.4, 0.4):
                freqs.append(f * ratio + detune)
                amps.append(amp / 2)
                decays.append(decay)
        track = place(track, ring(freqs, amps, decays, 5.0, 0.2, (600, 5000), 150.0) * level, at)
    return finish(room(track, 2.0, 0.2, 4000), 0.85, 0.1)


def chimes(strikes, seconds=3.2):
    """Wind chimes: tubes tuned to a pentatonic scale, struck at random by the wind."""
    notes = [987.8, 1108.7, 1318.5, 1480.0, 1661.2, 1975.5]
    track = silence(seconds)
    at = 0.05
    for _ in range(strikes):
        f = notes[rng.integers(0, len(notes))]
        tube = ring([f, f * 2.756, f * 5.404], [1.0, 0.45, 0.2], [2.2, 0.9, 0.35], 2.6, 0.06, (3000, 10000), 500.0)
        track = place(track, tube * (0.4 + 0.6 * rng.random()), at)
        at += 0.12 + 0.35 * rng.random()
    return finish(room(track, 1.5, 0.25, 7000), 0.7, 0.1)


# ---------------------------------------------------------------- the war horn
def horn_note(f, length, attack=0.3, bend_end=0.0):
    t = t_axis(length)
    vib = 1.0 + 0.005 * np.sin(2 * np.pi * 4.8 * t) * np.clip(t * 1.5, 0, 1)
    scoop = 1.0 - 0.04 * np.exp(-t * 7.0)
    fall = 1.0 - bend_end * np.clip((t - (length - 0.5)) / 0.5, 0, 1) ** 2
    phase = 2 * np.pi * np.cumsum(f * vib * scoop * fall) / RATE
    env = np.clip(t / attack, 0, 1) ** 1.5 * np.clip((length - t) / 0.35, 0, 1)
    dark = sum(np.sin(k * phase) / k ** 1.4 for k in range(1, 7))
    bright = sum(np.sin(k * phase) / k for k in range(1, min(40, int(RATE / 2 / (f * 1.1)))))
    tone = dark * (1.0 - env) + bright * env           # brassier the harder it is blown
    breath = band(rng.standard_normal(len(t)), f * 3, f * 12) * 0.04
    return (tone * 0.25 + breath) * env


def war_horn(notes):
    track = silence(0.05)
    for at, f, length, bend in notes:
        track = place(track, horn_note(f, length, bend_end=bend), at)
    return finish(lowpass(room(track, 2.6, 0.35, 2200), 6000), 0.85, 0.2)


# ---------------------------------------------------------------- wood and earth
def axe_hit(level=1.0, split=False):
    t = t_axis(0.7)
    thump = np.sin(2 * np.pi * (165 + 70 * np.exp(-t * 35)) * t) * np.exp(-t / 0.07)
    knock = np.sin(2 * np.pi * 430 * t) * np.exp(-t / 0.04) * 0.5 + np.sin(2 * np.pi * 780 * t) * np.exp(-t / 0.025) * 0.3
    crack = band(rng.standard_normal(len(t)), 1200, 5000) * np.exp(-t * (40 if split else 90)) * (1.4 if split else 0.8)
    out = thump + knock + crack
    if split:   # the halves fall apart
        for at in (0.18, 0.27):
            piece = t_axis(0.3)
            fall = np.sin(2 * np.pi * 240 * piece) * np.exp(-piece / 0.05) * 0.5 + band(rng.standard_normal(len(piece)), 300, 2000) * np.exp(-piece * 60) * 0.3
            out = place(out, fall, at)
    return fade(out, 0.0003, 0.05) * level


def chop(hits):
    track = silence(0.05)
    for at, level, split in hits:
        track = place(track, axe_hit(level, split), at)
    return finish(room(track, 0.9, 0.18, 3000))


def grains(seconds, density, cutoff=2500):
    """Crumbling earth: a soft low rush full of tiny clicks."""
    n = int(seconds * RATE)
    clicks = np.zeros(n)
    idx = rng.integers(0, n, int(density * seconds))
    clicks[idx] = rng.standard_normal(len(idx))
    return lowpass(clicks * 2.0 + rng.standard_normal(n) * 0.15, cutoff)


def hoe_stroke(level=1.0):
    t = t_axis(0.45)
    bite = grains(0.45, 900, 3000) * np.exp(-t * 9.0)
    thud = np.sin(2 * np.pi * 105 * t) * np.exp(-t / 0.05) * 0.5
    return fade(bite + thud, 0.002, 0.05) * level


def drag(seconds=0.6):
    t = t_axis(seconds)
    return fade(grains(seconds, 500, 1800) * np.sin(np.pi * t / seconds) ** 0.7 * 0.7, 0.02, 0.05)


def farm(parts):
    track = silence(0.05)
    for at, kind in parts:
        track = place(track, hoe_stroke(0.8 + 0.2 * rng.random()) if kind == 'hoe' else drag(), at)
    return finish(room(track, 0.4, 0.08, 4000), 0.75)


def whetstone_stroke(length=0.5):
    t = t_axis(length)
    swell = np.sin(np.pi * t / length) ** 1.5
    scrape = band(rng.standard_normal(len(t)), 2500, 8000) * swell * 0.6
    grit = band(rng.standard_normal(len(t)), 900, 2500) * swell * 0.25
    out = scrape + grit
    blade = ring([3300, 4750, 6150], [0.5, 0.35, 0.2], [0.45, 0.3, 0.2], 0.9, 0.0)   # the blade rings as it leaves
    return place(out, blade, length * 0.85)


def sharpen(strokes):
    track = silence(0.05)
    for at, length in strokes:
        track = place(track, whetstone_stroke(length), at)
    return finish(room(track, 0.5, 0.1, 7000), 0.7)


# ---------------------------------------------------------------- drums, pipe, night, birds
def drum(f_start, f_end, decay, length=1.2, slap=0.4, overtone=0.35):
    """A skin drum: a sine whose pitch falls after the hit, a faster overtone, and a slap of filtered noise."""
    t = t_axis(length)
    freq = f_end + (f_start - f_end) * np.exp(-t * 18.0)
    body = np.sin(2 * np.pi * np.cumsum(freq) / RATE) * np.exp(-t / decay)
    body += overtone * np.sin(2 * np.pi * np.cumsum(freq * 1.59) / RATE) * np.exp(-t / (decay * 0.45))
    hit = band(rng.standard_normal(len(t)), 150, 2200) * np.exp(-t * 45.0) * slap
    return fade(body + hit, 0.0005, 0.05)


def rim(length=0.12):
    t = t_axis(length)
    return fade(band(rng.standard_normal(len(t)), 1800, 6500) * np.exp(-t * 70.0) * 0.5
                + np.sin(2 * np.pi * 820 * t) * np.exp(-t * 60.0) * 0.25, 0.0005, 0.02)


def war_drums(pattern, bpm):
    """pattern: one string per bar of sixteenths: B big drum, M middle drum, r rim click, . rest."""
    step = 60.0 / bpm / 4
    sounds = {'B': lambda: drum(110, 52, 0.28, slap=0.6) * (0.9 + 0.1 * rng.random()),
              'M': lambda: drum(190, 128, 0.15, length=0.8, slap=0.7) * 0.55,
              'r': lambda: rim() * 0.6}
    track = silence(0.1)
    at = 0.05
    for bar in pattern:
        for ch in bar:
            if ch in sounds:
                track = place(track, sounds[ch](), at + rng.normal(0, 0.004))
            at += step
    track = place(track, silence(1.0), at)
    return normalize(fade(lowpass(room(track, 1.2, 0.12, 1800), 5000), 0.001, 0.6))


A4 = 440.0
PENTA = {'A3': -12, 'C4': -9, 'D4': -7, 'E4': -5, 'G4': -2, 'A4': 0, 'C5': 3, 'D5': 5, 'E5': 7, 'G5': 10, 'A5': 12}


def pipe_note(freq, length, breath=0.12):
    t = t_axis(length)
    vib = 1.0 + 0.006 * np.sin(2 * np.pi * 5.3 * t) * np.clip(t * 4, 0, 1)
    phase = 2 * np.pi * np.cumsum(freq * vib) / RATE
    tone = np.sin(phase) + 0.28 * np.sin(2 * phase) + 0.08 * np.sin(3 * phase)
    air = band(rng.standard_normal(len(t)), freq * 0.8, freq * 4.0) * breath
    env = np.minimum(1.0, t / 0.035) * np.minimum(1.0, (length - t) / 0.07)
    return (tone + air) * np.clip(env, 0, 1)


def pipe_tune(notes, bpm, octave=1.0):
    """notes: (pitch name or '-', length in eighths)."""
    eighth = 60.0 / bpm / 2
    track = silence(0.05)
    at = 0.05
    for name, eighths in notes:
        length = eighths * eighth
        if name != '-':
            freq = A4 * 2 ** (PENTA[name] / 12) * octave
            track = place(track, pipe_note(freq, length * 0.95) * 0.6, at)
        at += length
    track = place(track, silence(0.8), at)
    return normalize(fade(room(track, 1.1, 0.22, 4000), 0.005, 0.4))


def crickets(seconds, voices):
    track = silence(seconds)
    for carrier, period, pulses, level in voices:
        at = rng.random() * period
        while at < seconds - 0.3:
            chirp = silence(0.0)
            for k in range(pulses):
                t = t_axis(0.014)
                pulse = np.sin(2 * np.pi * carrier * t) * np.sin(np.pi * t / 0.014) ** 2
                chirp = place(chirp, pulse, k * 0.026)
            track = place(track, chirp * level * (0.8 + 0.2 * rng.random()), at)
            at += period * (0.9 + 0.2 * rng.random())
    return normalize(fade(room(track[:int(seconds * RATE)], 0.5, 0.15, 7000), 0.3, 0.6), 0.7)


def hoot(freq, length, bend=1.06):
    t = t_axis(length)
    f = freq * (1 + (bend - 1) * np.exp(-t * 12.0))
    tone = np.sin(2 * np.pi * np.cumsum(f) / RATE) + 0.15 * np.sin(4 * np.pi * np.cumsum(f) / RATE)
    air = lowpass(rng.standard_normal(len(t)), 900) * 0.25
    env = np.minimum(1.0, t / 0.06) * np.clip((length - t) / 0.12, 0, 1)
    return (tone + air) * env


def owl(calls):
    track = silence(0.05)
    for at, freq, length in calls:
        track = place(track, hoot(freq, length), at)
    return normalize(fade(room(track, 1.6, 0.35, 1500), 0.01, 0.5), 0.75)


def sweep(f0, f1, length, shape=1.0):
    t = t_axis(length)
    f = f0 + (f1 - f0) * (t / length) ** shape
    env = np.sin(np.pi * t / length) ** 0.6
    return np.sin(2 * np.pi * np.cumsum(f) / RATE) * env


def birds(phrases):
    """phrases: (start, kind, base frequency); kinds: whistle, trill, sweep_up, tee_yoo."""
    track = silence(0.05)
    for at, kind, f in phrases:
        if kind == 'trill':
            s = silence(0.0)
            for k in range(7):
                s = place(s, sweep(f * (1.12 if k % 2 else 1.0), f * (1.12 if k % 2 else 1.0) * 0.97, 0.045), k * 0.05)
        elif kind == 'sweep_up':
            s = sweep(f * 0.7, f * 1.25, 0.09, 0.7)
        elif kind == 'tee_yoo':
            s = place(sweep(f, f * 1.25, 0.12), sweep(f * 1.2, f * 0.85, 0.2), 0.14)
        else:
            s = sweep(f, f * 1.02, 0.18)
        track = place(track, s * (0.6 + 0.4 * rng.random()), at)
    return normalize(fade(room(track, 0.7, 0.2, 9000), 0.01, 0.3), 0.6)


# ---------------------------------------------------------------- snores
def rattle(length, low=90, high=420, buzz=28.0):
    """A snoring breath in: dark noise shaken by the soft palate (a soft flutter, not a hard square)."""
    t = t_axis(length)
    breath = band(rng.standard_normal(len(t)), low, high, order=3)
    flutter = 0.5 + 0.5 * np.sin(2 * np.pi * buzz * t * (1 + 0.15 * t / length)) ** 2
    env = np.sin(np.pi * np.clip(t / length, 0, 1)) ** 1.4
    return lowpass(breath * flutter, 650) * env * 3.0


def snore(ending):
    track = rattle(1.0)
    if ending == 'whistle':
        # The whistle out wavers and is half breath, so two goblins whistling at once do not beat against each other.
        t = t_axis(0.7)
        wobble = 1050 + 160 * np.sin(np.pi * t / 0.7) + 25 * np.sin(2 * np.pi * 6.0 * t) + 15 * rng.standard_normal(len(t)).cumsum() / np.sqrt(len(t))
        whistle = np.sin(2 * np.pi * np.cumsum(wobble) / RATE)
        air = band(rng.standard_normal(len(t)), 700, 2600) * 0.35
        track = place(track, (whistle * 0.18 + air) * np.sin(np.pi * t / 0.7) ** 2, 1.25)
    elif ending == 'lips':
        t = t_axis(0.55)
        lips = band(rng.standard_normal(len(t)), 200, 1600) * (0.5 + 0.5 * np.sin(2 * np.pi * 34 * t) ** 2)
        track = place(track, lips * np.sin(np.pi * t / 0.55) ** 1.5 * 0.7, 1.2)
    else:   # a snort: two short rattles
        track = place(track, rattle(0.35, 120, 600, 36.0) * 1.1, 1.15)
        track = place(track, rattle(0.3, 120, 600, 40.0), 1.6)
    track = place(track, silence(0.3), len(track) / RATE)
    return normalize(fade(track, 0.02, 0.2), 0.8)


# ---------------------------------------------------------------- the set
def sounds():
    return {
        'war_drums_1': lambda: war_drums(['B...B...B.B.B...', 'B...B...BrrrB.B.', 'B...B...B.B.B...', 'B.M.B.M.BMMMB...'], 104),
        'war_drums_2': lambda: war_drums(['B..rB..rB.MMB..r', 'B..rB..rB.MMB...', 'B.B.B.B.MMMMB...'], 96),
        'war_drums_3': lambda: war_drums(['B.......B.......', 'B...M...B...M...', 'B.M.B.M.B.M.BBB.', 'B...............'], 112),
        'war_horn_1': lambda: war_horn([(0.05, 98.0, 1.3, 0.0), (1.25, 146.8, 1.9, 0.06)]),
        'war_horn_2': lambda: war_horn([(0.05, 110.0, 2.6, 0.12)]),
        'forge_1': lambda: forge([(0.05, 'B', 1.0), (0.4, 't', 0.5), (0.55, 't', 0.35), (0.95, 'B', 1.0), (1.3, 't', 0.5), (1.45, 't', 0.35),
                                  (1.85, 'B', 0.9)]),
        'forge_2': lambda: forge([(0.05, 't', 0.6), (0.2, 't', 0.4), (0.35, 't', 0.3), (0.7, 'B', 1.0), (1.2, 'B', 1.0), (1.7, 'B', 0.8)]),
        'forge_3': lambda: forge([(0.05, 'B', 0.9), (0.5, 'B', 0.9), (0.95, 't', 0.6), (1.1, 't', 0.45), (1.25, 't', 0.3)]),
        'sharpen_1': lambda: sharpen([(0.05, 0.5), (0.75, 0.5)]),
        'sharpen_2': lambda: sharpen([(0.05, 0.35), (0.5, 0.35), (0.95, 0.6)]),
        'sharpen_3': lambda: sharpen([(0.05, 0.7)]),
        'chop_1': lambda: chop([(0.05, 0.9, False), (1.0, 1.0, False), (1.95, 1.0, True)]),
        'chop_2': lambda: chop([(0.05, 1.0, False), (0.9, 0.9, False)]),
        'chop_3': lambda: chop([(0.05, 1.0, True)]),
        'farm_1': lambda: farm([(0.05, 'hoe'), (0.6, 'hoe'), (1.15, 'hoe')]),
        'farm_2': lambda: farm([(0.05, 'hoe'), (0.5, 'drag')]),
        'farm_3': lambda: farm([(0.05, 'drag'), (0.8, 'hoe'), (1.3, 'hoe')]),
        'coins_1': lambda: coins('pour'),
        'coins_2': lambda: coins('count'),
        'coins_3': lambda: coins('purse'),
        'jingle_1': jingle,
        'jingle_2': jingle,
        'jingle_3': jingle,
        'bell_1': lambda: bell([(0.05, 1.0), (1.3, 0.9), (2.55, 0.85)]),
        'bell_2': lambda: bell([(0.05, 1.0), (1.4, 0.8)]),
        'chimes_1': lambda: chimes(9),
        'chimes_2': lambda: chimes(6),
        'chimes_3': lambda: chimes(12, 3.6),
        'pipe_tune_1': lambda: pipe_tune([('E5', 1), ('G5', 1), ('A5', 2), ('G5', 1), ('E5', 1), ('D5', 2), ('E5', 1), ('D5', 1),
                                          ('C5', 1), ('A4', 1), ('C5', 2), ('A4', 4)], 132),
        'pipe_tune_2': lambda: pipe_tune([('A4', 1), ('C5', 1), ('D5', 1), ('E5', 1), ('G5', 2), ('E5', 2), ('D5', 1), ('C5', 1),
                                          ('D5', 2), ('-', 1), ('A4', 1), ('G4', 1), ('A4', 3)], 144),
        'pipe_tune_3': lambda: pipe_tune([('D5', 2), ('A4', 1), ('D5', 1), ('E5', 2), ('G5', 2), ('E5', 1), ('D5', 1), ('E5', 1),
                                          ('C5', 1), ('D5', 4)], 120),
        'pipe_tune_4': lambda: pipe_tune([('G5', 1), ('E5', 1), ('G5', 1), ('A5', 1), ('G5', 1), ('E5', 1), ('D5', 2), ('C5', 1),
                                          ('D5', 1), ('E5', 1), ('G5', 1), ('E5', 4)], 150),
        'crickets_1': lambda: crickets(4.0, [(4600, 0.62, 3, 1.0), (4150, 0.85, 4, 0.6), (5050, 1.3, 3, 0.35)]),
        'crickets_2': lambda: crickets(4.0, [(4400, 0.5, 4, 0.9), (4900, 0.72, 3, 0.7)]),
        'owl_1': lambda: owl([(0.05, 380, 0.22), (0.42, 370, 0.18), (0.95, 360, 0.75)]),
        'owl_2': lambda: owl([(0.05, 410, 0.55), (0.9, 395, 0.2), (1.2, 390, 0.2), (1.55, 380, 0.6)]),
        'birds_1': lambda: birds([(0.05, 'tee_yoo', 3200), (0.6, 'tee_yoo', 3300), (1.4, 'trill', 3900)]),
        'birds_2': lambda: birds([(0.05, 'sweep_up', 3000), (0.2, 'sweep_up', 3200), (0.35, 'sweep_up', 3400), (0.9, 'whistle', 2800),
                                  (1.3, 'whistle', 3500)]),
        'birds_3': lambda: birds([(0.05, 'trill', 4200), (0.6, 'tee_yoo', 2900), (1.2, 'trill', 3600), (1.75, 'sweep_up', 3800)]),
        'snore_1': lambda: snore('whistle'),
        'snore_2': lambda: snore('lips'),
        'snore_3': lambda: snore('snort'),
    }


def write(path, x):
    """Ogg Vorbis in one-second pieces: libsndfile overflows its stack encoding a long buffer in one call."""
    with sf.SoundFile(path, 'w', RATE, 1, format='OGG', subtype='VORBIS') as out:
        for start in range(0, len(x), RATE):
            out.write(x[start:start + RATE].astype(np.float32))


def preview(made):
    from PIL import Image, ImageDraw
    w, row = 900, 40
    img = Image.new('RGB', (w, row * len(made)), (24, 26, 30))
    d = ImageDraw.Draw(img)
    for k, (name, x) in enumerate(made.items()):
        y0 = k * row + row // 2
        step = max(1, len(x) // (w - 150))
        for i in range(0, min(len(x), (w - 150) * step), step):
            amp = float(np.abs(x[i:i + step]).max()) * (row // 2 - 3)
            d.line([(150 + i // step, y0 - amp), (150 + i // step, y0 + amp)], fill=(120, 200, 110))
        d.text((6, y0 - 6), f'{name} {len(x) / RATE:.1f}s', fill=(230, 230, 230))
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    img.save(PREVIEW)


def demo(made, path=DEMO, only=None):
    """Sounds in a row with a pause between them, to listen to outside the game (with an index of where each starts)."""
    reel, at, index = silence(0.3), 0.3, []
    for name, x in made.items():
        if only and not name.startswith(only):
            continue
        minutes, tenths = divmod(round(at * 10), 600)
        index.append(f'{minutes}:{tenths / 10:04.1f}  {name}')
        reel = place(reel, x * 0.9, at)
        at += len(x) / RATE + 0.9
    write(path, reel)
    path.with_suffix('.txt').write_text('\n'.join(index) + '\n', encoding='utf-8')
    return index


def main():
    global rng
    OUT.mkdir(parents=True, exist_ok=True)
    made = {}
    for name, make in sounds().items():
        rng = np.random.default_rng(zlib.crc32(name.encode()))
        x = make()
        assert np.isfinite(x).all() and np.abs(x).max() <= 1.0, name
        made[name] = x
        write(OUT / f'{name}.ogg', x)
    print(f'{len(made)} sounds, {sum(len(x) for x in made.values()) / RATE:.0f} s in all')
    preview(made)
    for line in demo(made):
        print(line)
    print('waveforms:', PREVIEW, ' demo:', DEMO)


if __name__ == '__main__':
    main()
