class Literal(object):

    @classmethod
    def fromDict(self, aDictionary):
        return Literal(
            value=aDictionary['value'], 
            lang=aDictionary.get('xml:lang', None))

    def __init__(self, value, lang=None):
        self._value = value
        self._lang = lang

    def __eq__(self, other):
        return (isinstance(other, self.__class__) and self.__dict__ == other.__dict__)

    def __ne__(self, other):
        return not self.__eq__(other)


    def value(self):
        return self._value

    def lang(self):
        return self._lang


