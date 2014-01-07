/* begin license *
 *
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server.
 *
 * Copyright (C) 2011-2014 Seecr (Seek You Too B.V.) http://seecr.nl
 *
 * This file is part of "Meresco Owlim"
 *
 * "Meresco Owlim" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * "Meresco Owlim" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with "Meresco Owlim"; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * end license */

package org.meresco.triplestore;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import org.w3c.dom.Document;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.codec.binary.Base64;

public class TransactionItem {
    private String action, identifier, filedata;

    private static XPathExpression actionXPath;
    private static XPathExpression identifierXPath;
    private static XPathExpression filedataXPath;

    static {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        try {
            actionXPath = xpathFactory.newXPath().compile("/transaction_item/action/text()");
            identifierXPath = xpathFactory.newXPath().compile("/transaction_item/identifier/text()");
            filedataXPath = xpathFactory.newXPath().compile("/transaction_item/filedata/text()");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TransactionItem(String action, String identifier, String filedata) {
        this.action = action;
        this.identifier = identifier;
        this.filedata = filedata;
    }

    public static TransactionItem read(String tsItem) throws Exception {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            Document doc = domFactory.newDocumentBuilder().parse(new ReaderInputStream(new StringReader(tsItem)));
            return new TransactionItem(
                actionXPath.evaluate(doc),
                identifierXPath.evaluate(doc),
                new String(Base64.decodeBase64(filedataXPath.evaluate(doc))));
        } catch (Exception e) {
            throw e;
        }
    }

    public String toString() {
        return "<transaction_item>" +
            "<action>" + this.action + "</action>" +
            "<identifier>" + StringEscapeUtils.escapeXml(this.identifier) + "</identifier>" +
            "<filedata>" + Base64.encodeBase64String(this.filedata.getBytes()) + "</filedata>" +
            "</transaction_item>\n";
    }

    public String getAction() {
        return this.action;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getFiledata() {
        return this.filedata;
    }
}
