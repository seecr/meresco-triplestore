class Uri(object):

    @classmethod
    def fromDict(self, aDictionary):
        return Uri(value=aDictionary['value'])

    def __init__(self, value):
        self._value = value
    
    def __eq__(self, other):
        return (isinstance(other, self.__class__) and self.__dict__ == other.__dict__)

    def __ne__(self, other):
        return not self.__eq__(other)

    def value(self):
        return self._value

