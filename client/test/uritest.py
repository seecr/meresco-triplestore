from cq2utils import CQ2TestCase

from meresco.owlim import Uri

class UriTest(CQ2TestCase):
    def testCreate(self):
        u = Uri.fromDict({"type": "uri", "value": "http://www.rnaproject.org/data/rnax/odw/InformationConcept"})
        self.assertEquals("http://www.rnaproject.org/data/rnax/odw/InformationConcept", u.value())
