"""Converts the raid alarm (tools/sounds/raid_alarm.mp3, provided by the user) to the Ogg Vorbis file Minecraft plays.

Run with a Python that has `soundfile` (libsndfile >= 1.1 reads MP3 and writes Vorbis):
    python tools/sounds/convert.py
"""
from pathlib import Path

import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'tools/sounds/raid_alarm.mp3'
OUT = ROOT / 'src/main/resources/assets/goblinraiders/sounds/raid_alarm.ogg'

data, rate = sf.read(SRC, dtype='float32')
if data.ndim > 1:
    data = data.mean(axis=1)                 # mono, so the game can play it like any other effect
peak = float(np.abs(data).max())
if peak > 0.95:
    data = data * (0.95 / peak)              # the source clips slightly
OUT.parent.mkdir(parents=True, exist_ok=True)
sf.write(OUT, data, rate, format='OGG', subtype='VORBIS')
check, check_rate = sf.read(OUT, dtype='float32')
print(f'{OUT}: {len(check) / check_rate:.2f} s, {check_rate} Hz, peak {np.abs(check).max():.3f}, {OUT.stat().st_size} bytes')
