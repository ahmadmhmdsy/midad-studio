#!/usr/bin/env python3
"""Verify that every native library in an AAB/APK is 16 KB page-size compatible.

Google Play requires ELF LOAD segments in apps targeting Android 15+ (API 35) to be
aligned to at least 16 KB, so the loader can map them on devices with 16 KB pages.
This script reads the program headers directly instead of trusting a build log.

Usage: python tools/check_16kb.py app/build/outputs/bundle/release/app-release.aab
Exit code 0 when every library passes, 1 otherwise.
"""
from __future__ import annotations

import struct
import sys
import zipfile

MIN_ALIGN = 16384
PT_LOAD = 1


def load_alignments(data: bytes) -> list[int]:
    if data[:4] != b"\x7fELF":
        raise ValueError("not an ELF file")
    is_64 = data[4] == 2
    little = data[5] == 1
    endian = "<" if little else ">"
    if is_64:
        phoff = struct.unpack_from(endian + "Q", data, 0x20)[0]
        phentsize = struct.unpack_from(endian + "H", data, 0x36)[0]
        phnum = struct.unpack_from(endian + "H", data, 0x38)[0]
    else:
        phoff = struct.unpack_from(endian + "I", data, 0x1C)[0]
        phentsize = struct.unpack_from(endian + "H", data, 0x2A)[0]
        phnum = struct.unpack_from(endian + "H", data, 0x2C)[0]
    aligns: list[int] = []
    for index in range(phnum):
        base = phoff + index * phentsize
        p_type = struct.unpack_from(endian + "I", data, base)[0]
        if p_type != PT_LOAD:
            continue
        if is_64:
            align = struct.unpack_from(endian + "Q", data, base + 0x30)[0]
        else:
            align = struct.unpack_from(endian + "I", data, base + 0x1C)[0]
        aligns.append(align)
    return aligns


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print("usage: check_16kb.py <app.aab|app.apk>")
        return 2
    path = argv[1]
    failures: list[str] = []
    checked = 0
    with zipfile.ZipFile(path) as archive:
        names = sorted(n for n in archive.namelist() if n.endswith(".so") and "/lib/" in n)
        if not names:
            print("FAIL: no native libraries found in", path)
            return 1
        abis = sorted({n.split("/")[2] for n in names if n.startswith("base/lib/") or n.startswith("lib/")})
        print(f"archive: {path}")
        print(f"abis: {', '.join(abis)}")
        for name in names:
            with archive.open(name) as handle:
                data = handle.read()
            try:
                aligns = load_alignments(data)
            except ValueError as error:
                failures.append(f"{name}: {error}")
                continue
            checked += 1
            smallest = min(aligns) if aligns else 0
            status = "OK" if smallest >= MIN_ALIGN else "FAIL"
            print(f"  [{status}] {name}: LOAD p_align=0x{smallest:X} ({len(aligns)} segments)")
            if smallest < MIN_ALIGN:
                failures.append(f"{name}: p_align 0x{smallest:X} < 0x{MIN_ALIGN:X}")
    print()
    if failures:
        for failure in failures:
            print("FAIL:", failure)
        return 1
    print(f"PASS: {checked} native libraries are 16 KB aligned")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
