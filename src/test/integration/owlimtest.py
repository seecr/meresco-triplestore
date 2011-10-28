
from integration import IntegrationTestCase

from urllib import urlopen

class OwlimTest(IntegrationTestCase):

    def testOne(self):
        self.assertTrue('"vars": [ "x" ]' in urlopen("http://localhost:%s/query?query=SELECT ?x WHERE {}" % self.owlimPort).read())
