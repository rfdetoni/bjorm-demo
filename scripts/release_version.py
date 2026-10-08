#!/usr/bin/env python3
"""Small release guard, with no dependencies outside the Python stdlib."""
import re
import sys
import xml.etree.ElementTree as ET


def current():
    root = ET.parse('pom.xml').getroot()
    return root.findtext('{http://maven.apache.org/POM/4.0.0}version')


if len(sys.argv) != 3 or sys.argv[1] not in ('check', 'next'):
    sys.exit('usage: release_version.py check|next MAJOR.MINOR.PATCH')
version = sys.argv[2]
if not re.fullmatch(r'(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)', version):
    sys.exit('Invalid semver')
if sys.argv[1] == 'check':
    if current() != version + '-SNAPSHOT':
        sys.exit('Expected current Maven version ' + version + '-SNAPSHOT; got ' + str(current()))
else:
    major, minor, patch = map(int, version.split('.'))
    print(f'{major}.{minor}.{patch + 1}-SNAPSHOT')
