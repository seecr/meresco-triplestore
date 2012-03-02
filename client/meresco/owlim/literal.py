## begin license ##
# 
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server. 
# 
# Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
# 
# This file is part of "Meresco Owlim"
# 
# "Meresco Owlim" is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
# 
# "Meresco Owlim" is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with "Meresco Owlim"; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
# 
## end license ##

class Literal(object):

    @classmethod
    def fromDict(self, aDictionary):
        return Literal(
            value=aDictionary['value'], 
            lang=aDictionary.get('xml:lang', None))

    def __init__(self, value, lang=None):
        self.value = value
        self.lang = lang

    def __eq__(self, other):
        return other.__class__ is self.__class__ and self.value == other.value and other.lang == self.lang

    def __str__(self):
        if self.lang:
            return "%s@%s" % (repr(self.value), self.lang)
        return self.value

    def __repr__(self):
        template = "%%s(%%s%s)"        
        template = template % (", lang=" + repr(self.lang) if self.lang else "")
        return template % (self.__class__.__name__, repr(self.value))

