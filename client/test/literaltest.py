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
