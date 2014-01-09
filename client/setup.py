## begin license ##
#
# The Meresco Triplestore package consists out of a HTTP server written in Java that
# provides access to an Triplestore with a Sesame Interface, as well as python bindings to
# communicate as a client with the server.
#
# Copyright (C) 2011-2014 Seecr (Seek You Too B.V.) http://seecr.nl
#
# This file is part of "Meresco Triplestore"
#
# "Meresco Triplestore" is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# "Meresco Triplestore" is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with "Meresco Triplestore"; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
#
## end license ##

from distutils.core import setup

setup(
    name='meresco-triplestore',
    packages=[
        'meresco',                  #DO_NOT_DISTRIBUTE
        'meresco.triplestore',
    ],
    version='%VERSION%',
    url='http://www.seecr.nl',
    author='Seecr (Seek You Too B.V.)',
    author_email='info@seecr.nl',
    description='A Python binding to communicate with an Sesame Interace HTTP Server.',
    long_description='A Python binding using HTTP to communicate with an Sesame Interface HTTP Server.',
    license='GNU Public License',
    platforms='all',
)
