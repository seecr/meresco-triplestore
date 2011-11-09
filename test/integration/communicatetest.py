from integration import IntegrationTestCase

from meresco.owlim import HttpClient

class CommunicateTest(IntegrationTestCase):
    def testOne(self):
        client = HttpClient(port=self.owlimPort)
