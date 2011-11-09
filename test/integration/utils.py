## begin license ##
# 
# All rights reserved.
# 
# Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
# 
## end license ##

from amara.binderytools import bind_string
from lxml.etree import parse as parse_lxml
from StringIO import StringIO
from socket import socket
from urllib import urlencode
from cq2utils.wrappers import wrapp
from sys import stdout

def _socket(port, timeOutInSeconds):
    sok = socket()
    sok.connect(('localhost', port))
    sok.settimeout(5.0 if timeOutInSeconds is None else timeOutInSeconds)
    return sok

def createReturnValue(header, body, parse):
    if parse == True:
        try:
            body = wrapp(bind_string(body))
        except:
            print body
            raise
    elif parse == 'lxml':
        try:
            body = parse_lxml(StringIO(body))
        except Exception, e:
            for lineNo, line in enumerate(body.split('\n')):
                print "%.5s %s" % (lineNo, line)
            print '\n', str(e)
    return header, body


def postRequest(port, path, data, contentType='text/xml; charset="utf-8"', parse=True, timeOutInSeconds=None):
    sok = _socket(port, timeOutInSeconds)
    try:
        contentLength = len(data)
        sendBuffer = '\r\n'.join([
            'POST %(path)s HTTP/1.0',
            'Content-Type: %s' % contentType,
            'Content-Length: %(contentLength)s',
            '', '']) % locals()
        sendBuffer += data

        totalBytesSent = 0
        bytesSent = 0
        while totalBytesSent != len(sendBuffer):
            bytesSent = sok.send(sendBuffer[totalBytesSent:])
            totalBytesSent += bytesSent

        header, body = receiveFromSocket(sok)
        return createReturnValue(header, body, parse)
    finally:
        sok.close()

def postMultipartForm(port, path, formValues, parse=True, timeOutInSeconds=None):
    boundary = '-=-=-=-=-=-=-=-=TestBoundary1234567890'
    body = createPostMultipartForm(boundary, formValues)
    return postRequest(port, path, body, contentType='multipart/form-data; boundary=' + boundary, parse=parse, timeOutInSeconds=timeOutInSeconds)

def createPostMultipartForm(boundary, formValues):
    strm = StringIO()
    for valueDict in formValues:
        fieldType = valueDict['type']
        headers = {}
        headers['Content-Disposition'] = 'form-data; name="%(name)s"' % valueDict
        if fieldType == 'file':
            headers['Content-Disposition'] = headers['Content-Disposition'] + '; filename="%(filename)s"' % valueDict
            headers['Content-Type'] = valueDict['mimetype']

        strm.write('--' + boundary + '\r\n')
        for item in headers.items():
            strm.write('%s: %s\r\n' % item)
        strm.write('\r\n')
        strm.write(valueDict['value'])
        strm.write('\r\n')
    strm.write('--' + boundary + '--\r\n')

    return strm.getvalue()

def getRequest(port, path, arguments, parse=True, timeOutInSeconds=None, host=None, additionalHeaders=None):
    sok = _socket(port, timeOutInSeconds)
    try:
        requestString = path
        if arguments:
            requestString = path + '?' + urlencode(arguments, doseq=True)

        request = 'GET %(requestString)s HTTP/1.0\r\n' % locals()
        if host != None:
            request = 'GET %(requestString)s HTTP/1.1\r\nHost: %(host)s\r\n' % locals()
        if additionalHeaders != None:
            for header in additionalHeaders.items():
                request += '%s: %s\r\n' % header
        request += '\r\n'

        sok.send(request)

        header, body = receiveFromSocket(sok)
        return createReturnValue(header, body, parse)
    finally:
        sok.close()


def receiveFromSocket(sok):
    response = ''
    part = sok.recv(1024)
    response += part
    while part != None:
        part = sok.recv(1024)
        if not part:
            break
        response += part
    return response.split('\r\n\r\n', 1)

