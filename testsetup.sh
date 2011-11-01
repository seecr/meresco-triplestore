## begin license ##
# 
# A Python binding using HTTP to communicate with an Owlim HTTP Server. 
# 
# Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
# 
# This file is part of "OwlimHttpClient"
# 
# "OwlimHttpClient" is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
# 
# "OwlimHttpClient" is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with "OwlimHttpClient"; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
# 
## end license ##

set -e

rm -rf tmp build

python setup.py install --root tmp
cp -r test tmp/test
find tmp -name '*.py' -exec sed '/DO_NOT_DISTRIBUTE/d' -i {} \;

export PYTHONPATH=`pwd`/tmp/usr/local/lib/python2.6/dist-packages

testtorun=$1
if [ -z "$testtorun" ]; then
    testtorun="alltests.sh"
fi

(
cd tmp/test
./$testtorun
)

rm -rf tmp build
