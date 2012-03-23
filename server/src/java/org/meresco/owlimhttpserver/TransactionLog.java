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

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

public class TransactionLog {
    TripleStore tripleStore;
    File transactionLogDir;
    File transactionLogFilePath;
    File tempLogDir;
    File committedFilePath;
    File committingFilePath;
    RandomAccessFile transactionLog;
    long maxSize;

    TransactionLog() {}

    public TransactionLog(TripleStore tripleStore, File baseDir, double maxSizeInMb) throws IOException {
        this(tripleStore, baseDir);
        this.maxSize = (long) (maxSizeInMb * (double)1024 * (double)1024);
    }

    public TransactionLog(TripleStore tripleStore, File baseDir) throws IOException {
        this.tripleStore = tripleStore;
        this.transactionLogDir = new File(baseDir, "transactionLog");
        this.transactionLogDir.mkdirs();
        this.transactionLogFilePath = new File(this.transactionLogDir, "current");
        System.out.println("Transaction log in " + this.transactionLogFilePath);
        this.tempLogDir = new File(baseDir, "tempLog");
        this.tempLogDir.mkdirs();
        clearTempLogDir();
        this.committedFilePath = new File(baseDir, "committed");
        this.committingFilePath = new File(baseDir, "committing");
    }

    public void init() throws Exception {
        recoverTripleStore();
        if (!this.committedFilePath.exists()) {
            this.committedFilePath.createNewFile();
        }
        this.transactionLog = new RandomAccessFile(this.transactionLogFilePath, "rwd");
    }

    public void add(String identifier, String filedata) throws TransactionLogException, IOException {
        doProcess("addRDF", identifier, filedata);
    }

    public void delete(String identifier) throws TransactionLogException, IOException {
        doProcess("delete", identifier, "");
    }

    void doProcess(String action, String identifier, String filedata) throws TransactionLogException, IOException {
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

        long originalPosition = this.transactionLog.getFilePointer();
        try {
            commit(filename);
        } catch (Exception e) {
            rollbackAll(filename, originalPosition);
            throw new TransactionLogException(e);
        }


        /* ROTATING */
    }

    String prepare(String action, String identifier, String filename, String filedata) throws Exception {
        File filepath = new File(this.tempLogDir, filename); 
        while (filepath.exists()) {
            filepath = new File(filepath + "_1");
        }
        new TransactionItem(action, identifier, filedata).write(filepath);
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

    void rollbackAll(String filename, long originalPosition) throws IOException {
        this.tripleStore.undoCommit();
        this.transactionLog.setLength(originalPosition);
        this.committingFilePath.renameTo(this.committedFilePath);
        rollback(filename);
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

    void clear() throws IOException {
       FileUtils.deleteDirectory(this.transactionLogDir);
       this.transactionLogDir.mkdir();
    }

    void clearTempLogDir() throws IOException {
        for (String filename: this.tempLogDir.list()) {
            rollback(filename);
        }
    }

    void persistTripleStore() throws Exception {
        this.tripleStore.shutdown();
        clear();
        this.tripleStore.startup();
    }

    boolean recoverTripleStore() throws Exception {
        String[] transactionItemFiles = getTransactionItemFiles();
        if (transactionItemFiles.length > 0) {
            System.out.println("Recovering " + String.valueOf(transactionItemFiles.length) + " files from transactionlog");
        } else {
            return false;
        }

        for(String filename: transactionItemFiles) {
            TransactionItem item = TransactionItem.read(new File (this.transactionLogDir, filename));
            if (item.getAction().equals("addRDF")) {
                this.tripleStore.addRDF(item.getIdentifier(), item.getFiledata());
            } else if (item.getAction().equals("delete")) {
                this.tripleStore.delete(item.getIdentifier());
            }
        }
        return true;
    }
}
