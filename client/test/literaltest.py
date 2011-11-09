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

from cq2utils import CQ2TestCase

from meresco.owlim import Literal

class LiteralTest(CQ2TestCase):
    def testWithoutLang(self):
        l = Literal.fromDict({"type": "literal", "value": "http://www.rnaproject.org/data/rnax/odw/InformationConcept"})
        self.assertEquals("http://www.rnaproject.org/data/rnax/odw/InformationConcept", l.value())
        self.assertEquals(None, l.lang())
    
    def testWithLang(self):
        l = Literal.fromDict({"type": "literal", "xml:lang": "eng", "value": "http://www.rnaproject.org/data/rnax/odw/InformationConcept"})
        self.assertEquals("http://www.rnaproject.org/data/rnax/odw/InformationConcept", l.value())
        self.assertEquals("eng", l.lang())

    def testEquals(self):
        l1 = Literal.fromDict({"type": "literal", "xml:lang": "eng", "value": "VALUE"})
        l2 = Literal.fromDict({"type": "literal", "xml:lang": "eng", "value": "VALUE"})
        self.assertEquals(l1, l2)
    
        l2 = Literal.fromDict({"type": "literal", "xml:lang": "dut", "value": "VALUE"})
        self.assertNotEqual(l1, l2)
        
        l2 = Literal.fromDict({"type": "literal", "value": "VALUE"})
        self.assertNotEqual(l1, l2)
        
        l2 = Literal.fromDict({"type": "literal", "value": "OTHER VALUE"})
        self.assertNotEqual(l1, l2)
