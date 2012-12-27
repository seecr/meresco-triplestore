## begin license ##
# 
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server. 
# 
# Copyright (C) 2011-2012 Seecr (Seek You Too B.V.) http://seecr.nl
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

from Ft.Xml.Lib import Uri as FtUri 

class Uri(str):
    def __init__(self, value):
        if isinstance(value, Uri):
            self.value = value.value
        else:
            self.value = value

    @classmethod
    def fromDict(self, valueDict):
        return Uri(valueDict['value'])

    def __repr__(self):
        return "%s(%s)" % (self.__class__.__name__, repr(self.value))

    def __eq__(self, other):
        return other.__class__ is self.__class__ and other.value == self.value

    @staticmethod
    def matchesUriSyntax(value):
        # should be replaced by check on the (broader) IRI syntax as supported in RDF. 
        return FtUri.MatchesUriSyntax(value)

