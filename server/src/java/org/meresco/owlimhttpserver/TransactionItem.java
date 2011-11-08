/* begin license *
 * 
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server. 
 * 
 * Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
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

package org.meresco.owlimhttpserver;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.apache.commons.lang3.StringEscapeUtils;

public class TransactionItem {
    String action, identifier, filedata;

    public TransactionItem(String action, String identifier, String filedata) {
        this.action = action;
        this.identifier = identifier;
        this.filedata = filedata;
    }

    public static TransactionItem read(File file) throws Exception {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            Document doc = domFactory.newDocumentBuilder().parse(file);
            XPathFactory factory = XPathFactory.newInstance();

            return new TransactionItem(
                factory.newXPath().evaluate("/transaction_item/action/text()", doc),
                factory.newXPath().evaluate("/transaction_item/identifier/text()", doc),
                factory.newXPath().evaluate("/transaction_item/filedata/text()", doc));
        } catch (Exception e) {
            throw new Exception(e);    
        }
    }

    public void write(File filepath) throws Exception {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filepath));
            out.write("<transaction_item>" +
                "<action>" + this.action + "</action>" +
                "<identifier>" + StringEscapeUtils.escapeXml(this.identifier) + "</identifier>" + 
                "<filedata>" + StringEscapeUtils.escapeXml(this.filedata) + "</filedata>" +
                "</transaction_item>");
            out.close();
        } catch (Exception e) {
            throw new Exception("Error: " + e.getMessage());
        }
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
