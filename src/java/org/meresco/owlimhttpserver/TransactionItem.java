
package org.meresco.owlimhttpserver;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class TransactionItem {
    String action, identifier, filedata;

    public static TransactionItem read(File file) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        TransactionItem item = new TransactionItem();
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        Document doc = domFactory.newDocumentBuilder().parse(file);

        XPathFactory factory = XPathFactory.newInstance();
        item.setAction(factory.newXPath().evaluate("/transaction_item/action/text()", doc));
        item.setIdentifier(factory.newXPath().evaluate("/transaction_item/identifier/text()", doc));
        item.setFiledata(factory.newXPath().evaluate("/transaction_item/filedata/text()", doc));

        return item;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getFiledata() {
        return this.filedata;
    }

    public void setFiledata(String filedata) {
        this.filedata = filedata;
    }
}
