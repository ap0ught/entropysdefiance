#!/usr/bin/env python3
# Copyright (c) 2024 Jonas Freiburghaus
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0

"""
Setup script for Entropy's Defiance TUI
"""

from setuptools import setup, find_packages
from pathlib import Path

# Read the README file
this_directory = Path(__file__).parent
long_description = (this_directory / "README.md").read_text()

setup(
    name="entropysdefiance-tui",
    version="0.1.0",
    description="Textual TUI for managing Entropy's Defiance tasks",
    long_description=long_description,
    long_description_content_type="text/markdown",
    author="Jonas",
    license="MPL-2.0",
    packages=find_packages(include=["entropysdefiance_tui"]),
    install_requires=[
        "textual>=0.41.0",
    ],
    entry_points={
        "console_scripts": [
            "entropysdefiance=entropysdefiance_tui.main:main",
            "ed-tui=entropysdefiance_tui.main:main",
        ],
    },
    python_requires=">=3.8",
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: End Users/Desktop",
        "License :: OSI Approved :: Mozilla Public License 2.0 (MPL 2.0)",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
    ],
)
