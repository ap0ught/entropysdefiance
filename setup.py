#!/usr/bin/env python3
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
    license="MIT",
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
        "License :: OSI Approved :: MIT License",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
    ],
)
