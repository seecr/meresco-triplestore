/* begin license *
 *
 * The Meresco Triplestore package consists out of a HTTP server written in Java that
 * provides access to an Triplestore with a Sesame Interface, as well as python bindings to
 * communicate as a client with the server.
 *
 * Copyright (C) 2011-2014, 2016 Seecr (Seek You Too B.V.) http://seecr.nl
 *
 * This file is part of "Meresco Triplestore"
 *
 * "Meresco Triplestore" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * "Meresco Triplestore" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with "Meresco Triplestore"; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * end license */

package org.meresco.triplestore;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.URI;
import org.openrdf.model.Resource;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFFormat;


public class TransactionLog implements Triplestore {
    Triplestore tripleStore;
    File transactionLogDir;
    File transactionLogFilePath;
    File committedFilePath;
    File committingFilePath;
    BufferedWriter transactionLog;
    long maxSize;

    private final static long DATESTAMP_FACTOR = 1000;
    private final static String CURRENT_TRANSACTIONLOG_NAME = "current";
    private static final int BUFFER_SIZE = 1024*1024;

    TransactionLog() {}

    public TransactionLog(Triplestore tripleStore, File baseDir, double maxSizeInMb) throws Exception {
        this(tripleStore, baseDir);
        this.maxSize = (long) (maxSizeInMb * (double)1024 * (double)1024);
    }

    public TransactionLog(Triplestore tripleStore, File baseDir) throws Exception {
        this.tripleStore = tripleStore;
        this.transactionLogDir = new File(baseDir, "transactionLog");
        this.transactionLogDir.mkdirs();
        this.transactionLogFilePath = new File(this.transactionLogDir, CURRENT_TRANSACTIONLOG_NAME);
        System.out.println("Transaction log in " + this.transactionLogFilePath);
        this.committedFilePath = new File(baseDir, "committed");
        this.committingFilePath = new File(baseDir, "committing");
        this.init();
        this.maxSize = (long) (500 * (double)1024 * (double)1024);
    }

    public void init() throws Exception {
        recoverTripleStore();
        if (!this.committedFilePath.exists()) {
            this.committedFilePath.createNewFile();
        }
        this.transactionLog = new BufferedWriter(new FileWriter(this.transactionLogFilePath), BUFFER_SIZE);
    }

    @Override
    public void add(String identifier, String data, RDFFormat format) throws RDFParseException {
        if (!format.equals(RDFFormat.RDFXML)) {
            throw new UnsupportedOperationException("Only RDFXML supported with transactionLog");
        }
        this.tripleStore.add(identifier, data, format);
        writeTransactionItem("add", identifier, data);
    }

    @Override
    public void addTriple(String tripleData) {
        this.tripleStore.addTriple(tripleData);
        writeTransactionItem("addTriple", "", tripleData);
    }

    @Override
    public void removeTriple(String tripleData) {
        this.tripleStore.removeTriple(tripleData);
        writeTransactionItem("removeTriple", "", tripleData);
    }

    @Override
    public void delete(String identifier) {
        this.tripleStore.delete(identifier);
        writeTransactionItem("delete", identifier, "");
    }

    @Override
    public void importTrig(String trig) {
        this.tripleStore.importTrig(trig);
        restartTripleStore();
    }

    public long size() {
        return this.tripleStore.size();
    }

    void writeTransactionItem(String action, String identifier, String filedata) {
        TransactionItem tsItem = new TransactionItem(action, identifier, filedata);
        try {
            commit(tsItem);
        } catch (Exception e) {
            System.err.println(e);
            throw new Error("Commit on transactionLog failed.", e);
        }
        maybeRotate();
    }

    void maybeRotate() {
        try {
            if (transactionLogFilePath.length() < this.maxSize) {
                return;
            }
            long newFilename = getTime();
            ArrayList<String> sortedTsFiles = getTransactionItemFiles();
            long lastAddedTimeStamp = sortedTsFiles.size() > 1 ? Long.valueOf(sortedTsFiles.get(sortedTsFiles.size() - 2)) : 0;
            if (newFilename <= lastAddedTimeStamp) { // in theory: only small differences by ntp
                return;
            }
            try {
                this.transactionLog.close();
                File newFile = new File(this.transactionLogDir, String.valueOf(newFilename));
                renameFileTo(this.transactionLogFilePath, newFile);
            } finally {
                this.transactionLog = new BufferedWriter(new FileWriter(this.transactionLogFilePath), BUFFER_SIZE);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void commit(TransactionItem tsItem) throws IOException {
        renameFileTo(this.committedFilePath, this.committingFilePath);
        commit_do(tsItem);
        renameFileTo(this.committingFilePath, this.committedFilePath);
    }

    void commit_do(TransactionItem tsItem) throws IOException {
        this.transactionLog.write(tsItem.toString());
        this.transactionLog.flush();
    }

    private void deleteFile(File filepath) throws IOException {
        if (!filepath.delete()) {
            throw new IOException("File could not be deleted.");
        }
    }

    private void renameFileTo(File from, File to) throws IOException {
        if (!from.renameTo(to)) {
            throw new IOException("File " + from.getAbsolutePath() + " could not be moved to " + to.getAbsolutePath());
        }
    }

    File getTransactionLogDir() {
        return this.transactionLogDir;
    }

    ArrayList<String> getTransactionItemFiles() {
        String[] transactionItems = this.transactionLogDir.list();
        ArrayList<Long> transactionItemsWithoutCurrent = new ArrayList<Long>();
        ArrayList<String> result = new ArrayList<String>();
        for (String t : transactionItems) {
            if (!t.equals(CURRENT_TRANSACTIONLOG_NAME)) {
                transactionItemsWithoutCurrent.add(Long.valueOf(t));
            }
        }
        Collections.sort(transactionItemsWithoutCurrent);
        for (Long t : transactionItemsWithoutCurrent) {
            result.add(String.valueOf(t));
        }
        if (Arrays.binarySearch(transactionItems, CURRENT_TRANSACTIONLOG_NAME) != -1) {
            result.add(CURRENT_TRANSACTIONLOG_NAME);
        }
        return result;
    }

    long getTime() {
        return System.currentTimeMillis() * DATESTAMP_FACTOR;
    }

    void clear() throws IOException {
        for (String filename : getTransactionItemFiles()) {
            deleteFile(new File(this.transactionLogDir, filename));
        }
    }

    void clear(File transactionItemFile) throws IOException {
        deleteFile(transactionItemFile);
    }

    void persistTripleStore(File transactionFile) throws Exception {
        this.tripleStore.shutdown();
        clear(transactionFile);
        this.tripleStore.startup();
    }

    void recoverTripleStore() throws Exception {
        ArrayList<String> transactionItemFiles = getTransactionItemFiles();
        if (transactionItemFiles.size() == 0) {
            return;
        }

        ArrayList<File> tsFiles = new ArrayList<File>();
        long recoverSize = 0;
        for (String s : transactionItemFiles) {
            File tsFile = new File(this.transactionLogDir, s);
            tsFiles.add(tsFile);
            recoverSize += tsFile.length();
        }
        System.out.println("Recovering " + recoverSize / 1024 / 1024 + "Mb from transactionLog");

        int totalCount = 0, totalSize = 0;
        long timeSpent = 0;
        long lastTime = 0;
        for (File f : tsFiles) {
            lastTime = new Date().getTime();
            int lineNo = 0;
            int itemCount = 0;
            BufferedLineReader blr = new BufferedLineReader(new FileReader(f));
            String line;
            StringBuilder tsItem = new StringBuilder();
            while ((line = blr.readLine()) != null) {
                lineNo += 1;
                tsItem.append(line);
                if (!line.contains("</transaction_item>")) {
                    continue;
                }
                itemCount += 1;
                try {
                    TransactionItem item = TransactionItem.read(tsItem.toString());
                    processTransactionItem(item);
                    timeSpent += new Date().getTime() - lastTime;
                    lastTime = new Date().getTime();
                    int itemLength = tsItem.length();
                    printProgress(itemLength, totalSize, timeSpent);
                    totalSize += itemLength;
                    tsItem = new StringBuilder();
                } catch (Exception e) {
                    System.err.println(e);
                    throw new TransactionLogException("Corrupted transaction_item in " + f.getAbsolutePath() + " at line " + lineNo + ". This should never occur.");
                }
            }
            if (tsItem.length() > 0) {
                if (!f.getName().equals(this.transactionLogFilePath.getName())) {
                    throw new TransactionLogException("Last TransactionLog item in " + f.getAbsolutePath() + " is corrupted. This should never occur.");
                } else if (!this.committingFilePath.exists()) {
                    throw new TransactionLogException("Last TransactionLog item is incomplete while not in the committing state. This should never occur.");
                }
            }
            if (itemCount > 0) {
                persistTripleStore(f);
            }
            totalCount += itemCount;
            blr.close();
        }
        if (this.committingFilePath.exists()) {
            deleteFile(this.committingFilePath);
        }
        System.out.println("Recovering of " + totalCount + " items completed.");
        return;
    }

    private void processTransactionItem(TransactionItem item) {
        String action = item.getAction();
        String identifier = item.getIdentifier();
        String data = item.getFiledata();
        try {
            if (action.equals("add")) {
                this.tripleStore.add(identifier, data, RDFFormat.RDFXML); //TODO: Only RDFXML in TransactionLog
            } else if (action.equals("delete")) {
                this.tripleStore.delete(identifier);
            } else if (action.equals("addTriple")) {
                this.tripleStore.addTriple(data);
            } else if (action.equals("removeTriple")) {
                this.tripleStore.removeTriple(data);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void printProgress(long newItemSize, long totalSize, long timeSpent) {
        long sizeInMb = totalSize / 1024 / 1024;
        long newSizeInMb = (totalSize + newItemSize) / 1024 / 1024;
        if (sizeInMb != newSizeInMb) {
            // print '.', newline for (multi)logging, ANSI cursor movements 1-up and n-right
            int nrOfDots = (int) (newSizeInMb % 50);
            if ( nrOfDots == 0 ) {
                nrOfDots = 50;
            }
            System.out.print(".\n\033[1A\033[" + nrOfDots + "C");
            if (newSizeInMb % 50 == 0) {
                System.out.println(" " + newSizeInMb + "Mb (" + timeSpent / newSizeInMb + "ms per Mb)");
            }
        }
        System.out.flush();
    }

    private void restartTripleStore() {
        System.out.println("Restarting triplestore. Please wait...");
        try {
            this.tripleStore.shutdown();
            clear();
            this.tripleStore.startup();
            System.out.println("Restart completed.");
            System.out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.flush();
            System.out.println("Restart failed.");
            System.out.flush();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String executeTupleQuery(String sparQL, TupleQueryResultFormat format) throws MalformedQueryException {
        return this.tripleStore.executeTupleQuery(sparQL, format);
    }

    @Override
    public String executeGraphQuery(String sparQL, RDFFormat format) throws MalformedQueryException {
        return this.tripleStore.executeGraphQuery(sparQL, format);
    }

    @Override
    public List<Namespace> getNamespaces() {
        return this.tripleStore.getNamespaces();
    }

    @Override
    public void shutdown() throws Exception {
        this.tripleStore.shutdown();
        this.clear();
    }

    @Override
    public void realCommit() throws Exception {
        this.tripleStore.realCommit();
    }

    @Override
    public void startup() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void export(String identifier) {
        this.tripleStore.export(identifier);
    }
}
