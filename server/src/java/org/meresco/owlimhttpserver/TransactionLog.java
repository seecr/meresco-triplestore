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
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

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

    final static long DATESTAMP_FACTOR = 1000;

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

    public void add(String identifier, String filedata) throws TransactionLogException, FileNotFoundException, IOException {
        doProcess("addRDF", identifier, filedata);
    }

    public void delete(String identifier) throws TransactionLogException, FileNotFoundException, IOException {
        doProcess("delete", identifier, "");
    }

    void doProcess(String action, String identifier, String filedata) throws TransactionLogException, FileNotFoundException, IOException {
        String filename = String.valueOf(getTime());
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

        maybeRotate(originalPosition);
    }

    void maybeRotate(long currentSize) throws FileNotFoundException, IOException {
        if (currentSize < this.maxSize) {
            return;
        }
        long newFilename = getTime();
        ArrayList<Long> timeStamps = new ArrayList<Long>();
        for (File file : this.transactionLogDir.listFiles()) {
            if (file.getName() == "current") {
                continue;
            }
            timeStamps.add(Long.valueOf(file.getName()));
        }
        Collections.sort(timeStamps);
        long lastAddedTimeStamp = timeStamps.size() > 0 ? timeStamps.get(timeStamps.size() - 1) : 0;
        if (newFilename < lastAddedTimeStamp) { // in theory: only small differences by ntp 
            return;
        }
        try {
            this.transactionLog.close();
            File newFile = new File(this.transactionLogDir, String.valueOf(newFilename));
            this.transactionLogFilePath.renameTo(newFile);
        } finally {
            this.transactionLog = new RandomAccessFile(this.transactionLogFilePath, "rwd");
        }
    }

    String prepare(String action, String identifier, String filename, String filedata) throws Exception {
        File filepath = new File(this.tempLogDir, filename); 
        while (filepath.exists()) {
            filepath = new File(filepath + "_1");
        }
        new TransactionItem(action, identifier, filedata).write(filepath);
        return filepath.getName();
    }

    void commit(String filename) throws IOException {
        this.committedFilePath.renameTo(this.committingFilePath);
        commit_do(filename);
        this.committingFilePath.renameTo(this.committedFilePath);
    }

    void commit_do(String filename) throws IOException {
        File tmpFilepath = new File(this.tempLogDir, filename); 
        BufferedReader br = new BufferedReader(new FileReader(tmpFilepath));
        String line;
        while ((line = br.readLine()) != null)   {
            this.transactionLog.writeChars(line);
        }
        tmpFilepath.delete();
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

    long getTime() {
        return System.currentTimeMillis() * DATESTAMP_FACTOR;
    }

    void clear() throws IOException {
        for (String filename : getTransactionItemFiles()) {
            new File(this.transactionLogDir, filename).delete();
        }
    }

    void clear(File transactionItemFile) throws IOException {
        transactionItemFile.delete();
    }

    void clearTempLogDir() throws IOException {
        for (String filename: this.tempLogDir.list()) {
            rollback(filename);
        }
    }

    void persistTripleStore(File transactionFile) throws Exception {
        this.tripleStore.shutdown();
        clear(transactionFile);
        this.tripleStore.startup();
    }

    boolean recoverTripleStore() throws Exception {
        String[] transactionItemFiles = getTransactionItemFiles();
        if (transactionItemFiles.length == 0) {
            return true;//MOET WEG
        }

        ArrayList<File> tsFiles = new ArrayList<File>();
        long recoverSize = 0;
        for (String s : transactionItemFiles) {
            File tsFile = new File(this.transactionLogDir, s);
            tsFiles.add(tsFile);
            recoverSize += tsFile.length();
        }
        System.out.println("Recovering " + recoverSize + "Mb from transactionLog");

        int totalCount = 0, totalSize = 0;
        String tsItem = "";
        for (File f : tsFiles) {
            int count = 0;
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null)   {
                tsItem += line; 
                if (!line.contains("</transaction_item>")) {
                    continue;
                }
                count += 1;
                totalSize += tsItem.length();
                try {
                    TransactionItem item = TransactionItem.read(tsItem);
                    if (item.getAction().equals("addRDF")) {
                        this.tripleStore.addRDF(item.getIdentifier(), item.getFiledata());
                    } else if (item.getAction().equals("delete")) {
                        this.tripleStore.delete(item.getIdentifier());
                    }
                    tsItem = "";
                } catch (Exception e) {
                    System.err.println(e);
                    throw new TransactionLogException("Corrupted transaction_item in " + f.getName() + ". This should never occur.");
                }
            }
            if (!tsItem.equals("")) {
                if (!f.getName().equals(this.transactionLogFilePath.getName())) {
                    throw new TransactionLogException("Last TransactionLog item in " + f.getName() + " is corrupted. This should never occur.");
                } else if (!this.committingFilePath.exists()) {
                    throw new TransactionLogException("Last TransactionLog item is incomplete while not in the committing state. This should never occur.");
                }
            }
            if (count > 0) {
                persistTripleStore(f);
            }
            totalCount += count;
        }
        if (this.committingFilePath.exists()) {
            this.committingFilePath.delete();
        }
        System.out.println("Recovering of " + totalCount + " items completed.");
        return false;//NIET NODIG MOET WEG
    }

    /*boolean xxxxrecoverTripleStore() throws Exception {
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
    }*/
}
