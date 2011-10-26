
package org.meresco.owlimhttpserver;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;

public class TransactionLog {
    File transactionLogDir;
    File tempLogDir;

    public TransactionLog(File baseDir) {
        this.transactionLogDir = new File(baseDir, "transactionLog");
        this.transactionLogDir.mkdir();
        this.tempLogDir = new File(baseDir, "tempLog");
        this.tempLogDir.mkdir();
    }

    File prepare(String action, String identifier, String filename, String filedata) {
        File filepath = new File(this.tempLogDir, filename); 
        int count = 0;
        while (filepath.exists()) {
            filepath = new File(this.tempLogDir, filename + "_" + String.valueOf(count++));
        }

        try {
            FileWriter fstream = new FileWriter(filepath);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("<transaction_item>" +
                "<action>addRDF</action>" +
                "<identifier>testRecord</identifier>" + 
                "<filedata>" + filedata + "</filedata>" +
                "</transaction_item>");
            out.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        return filepath;
    }

    void commit(String filename) {
        File tmpFilepath = new File(this.tempLogDir, filename); 
        File filepath = new File(this.transactionLogDir, filename);
        int count = 0;
        while (filepath.exists()) {
            filepath = new File(this.transactionLogDir, filename + "_" + String.valueOf(count++));
        }
        tmpFilepath.renameTo(filepath);
    }

    File getTempLogDir() {
        return this.tempLogDir;
    }

    File getTransactionLogDir() {
        return this.transactionLogDir;
    }

    String[] getTransactionItemFiles() {
        return this.transactionLogDir.list();
    }
}
