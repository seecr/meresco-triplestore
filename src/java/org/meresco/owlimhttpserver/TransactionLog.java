
package org.meresco.owlimhttpserver;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Arrays;

import org.apache.commons.lang3.StringEscapeUtils;

public class TransactionLog {
    TripleStore tripleStore;
    File transactionLogDir;
    File tempLogDir;

    TransactionLog() {}

    public TransactionLog(TripleStore tripleStore, File baseDir) {
        this.tripleStore = tripleStore;
        this.transactionLogDir = new File(baseDir, "transactionLog");
        this.transactionLogDir.mkdir();
        this.tempLogDir = new File(baseDir, "tempLog");
        this.tempLogDir.mkdir();
    }

    public void add(String identifier, String filedata) throws TransactionLogException {
        doProcess("addRDF", identifier, filedata);
    }

    public void delete(String identifier) throws TransactionLogException {
        doProcess("delete", identifier, "");
    }

    void doProcess(String action, String identifier, String filedata) throws TransactionLogException {
        String filename = getTime();
        try {
            filename = prepare(action, identifier, filename, filedata);
            if (action.equals("addRDF")) {
                this.tripleStore.addRDF(identifier, filedata);
            } else if (action.equals("delete")) {
                this.tripleStore.delete(identifier);
            }
        } catch (Exception e) {
            rollback(filename);
            throw new TransactionLogException(e);
        }
        commit(filename);
    }

    String prepare(String action, String identifier, String filename, String filedata) {
        File filepath = new File(this.tempLogDir, filename); 
        while (filepath.exists()) {
            filepath = new File(filepath + "_1");
        }

        try {
            FileWriter fstream = new FileWriter(filepath);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("<transaction_item>" +
                "<action>" + action + "</action>" +
                "<identifier>" + StringEscapeUtils.escapeXml(identifier) + "</identifier>" + 
                "<filedata>" + StringEscapeUtils.escapeXml(filedata) + "</filedata>" +
                "</transaction_item>");
            out.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        return filepath.getName();
    }

    void commit(String filename) {
        File tmpFilepath = new File(this.tempLogDir, filename); 
        File filepath = new File(this.transactionLogDir, filename);
        while (filepath.exists()) {
            filepath = new File(filepath + "_1");
        }
        tmpFilepath.renameTo(filepath);
    }

    void rollback(String filename) {
        new File(this.tempLogDir, filename).delete();
    }

    File getTempLogDir() {
        return this.tempLogDir;
    }

    File getTransactionLogDir() {
        return this.transactionLogDir;
    }

    String[] getTransactionItemFiles() {
        String[] transactionItems = this.transactionLogDir.list();
        Arrays.sort(transactionItems);
        return transactionItems;
    }

    String getTime() {
        return String.valueOf(System.currentTimeMillis());
    }
}
