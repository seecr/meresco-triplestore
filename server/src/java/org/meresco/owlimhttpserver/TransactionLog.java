/* begin license *
 * 
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server. 
 * 
 * Copyright (C) 2011-2012 Seecr (Seek You Too B.V.) http://seecr.nl
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import org.openrdf.model.Value;
import org.openrdf.model.URI;
import org.openrdf.model.Resource;

public class TransactionLog {
    TripleStore tripleStore;
    File transactionLogDir;
    File transactionLogFilePath;
    @Deprecated
    File tempLogDir;
    File committedFilePath;
    File committingFilePath;
    BufferedWriter transactionLog;
    long maxSize;

    private final static long DATESTAMP_FACTOR = 1000;
    private final static String CURRENT_TRANSACTIONLOG_NAME = "current";
    private static final int BUFFER_SIZE = 1024*1024;

    TransactionLog() {}

    public TransactionLog(TripleStore tripleStore, File baseDir, double maxSizeInMb) throws IOException {
        this(tripleStore, baseDir);
        this.maxSize = (long) (maxSizeInMb * (double)1024 * (double)1024);
    }

    public TransactionLog(TripleStore tripleStore, File baseDir) throws IOException {
        this.tripleStore = tripleStore;
        this.transactionLogDir = new File(baseDir, "transactionLog");
        this.transactionLogDir.mkdirs();
        this.transactionLogFilePath = new File(this.transactionLogDir, CURRENT_TRANSACTIONLOG_NAME);
        System.out.println("Transaction log in " + this.transactionLogFilePath);
        this.tempLogDir = new File(baseDir, "tempLog");
        clearTempLogDir();
        this.committedFilePath = new File(baseDir, "committed");
        this.committingFilePath = new File(baseDir, "committing");
        this.maxSize = (long) (500 * (double)1024 * (double)1024);
    }

    public void init() throws Exception {
        recoverTripleStore();
        if (!this.committedFilePath.exists()) {
            this.committedFilePath.createNewFile();
        }
        this.transactionLog = new BufferedWriter(new FileWriter(this.transactionLogFilePath), BUFFER_SIZE);
    }

    public void add(String identifier, String filedata) throws TransactionLogException, IOException {
        doProcess("addRDF", identifier, filedata);
    }

    public void addTriple(String filedata) throws TransactionLogException, IOException {
        doProcess("addTriple", "", filedata);
    }

    public void removeTriple(String filedata) throws TransactionLogException, IOException {
        doProcess("removeTriple", "", filedata);
    }

    public void delete(String identifier) throws TransactionLogException, IOException {
        doProcess("delete", identifier, "");
    }

    void doProcess(String action, String identifier, String filedata) throws TransactionLogException, FileNotFoundException, IOException {
    	TransactionItem tsItem = new TransactionItem(action, identifier, filedata);
        try {
            if (action.equals("addRDF")) {
                this.tripleStore.addRDF(identifier, filedata);
            } else if (action.equals("delete")) {
                this.tripleStore.delete(identifier);
            } else if (action.equals("addTriple")) {
                this.tripleStore.addTriple(filedata);
            } else if (action.equals("removeTriple")) {
                this.tripleStore.removeTriple(filedata);
            }
        } catch (Exception e) {
            throw new TransactionLogException(e);
        }

        try {
            commit(tsItem);
        } catch (Exception e) {
        	rollback();
        	System.err.println(e);
            throw new Error("Commit on transactionLog failed.", e);
        }

        maybeRotate();
    }

    void maybeRotate() throws FileNotFoundException, IOException {
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

    void rollback() {
        this.tripleStore.undoCommit();
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

    @Deprecated
    void clearTempLogDir() throws IOException {
    	if (!this.tempLogDir.exists()) {
    		return;
    	}
        for (String filename: this.tempLogDir.list()) {
            deleteFile(new File(this.tempLogDir, filename));
        }
        deleteFile(this.tempLogDir);
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
            int count = 0;
            BufferedLineReader blr = new BufferedLineReader(new FileReader(f));
            String line;
            StringBuilder tsItem = new StringBuilder();
            while ((line = blr.readLine()) != null) {
            	tsItem.append(line);
            	if (!line.contains("</transaction_item>")) {
                    continue;
                }
                count += 1;
                try {
                	TransactionItem item = TransactionItem.read(tsItem.toString());
                    if (item.getAction().equals("addRDF")) {
                        this.tripleStore.addRDF(item.getIdentifier(), item.getFiledata());
                    } else if (item.getAction().equals("delete")) {
                        this.tripleStore.delete(item.getIdentifier());
                    }
                    timeSpent += new Date().getTime() - lastTime;
                    lastTime = new Date().getTime();
                    int itemLength = tsItem.length();
                    printProgress(itemLength, totalSize, timeSpent);
                    totalSize += itemLength;
                    tsItem = new StringBuilder();
                } catch (Exception e) {
                    System.err.println(e);
                    throw new TransactionLogException("Corrupted transaction_item in " + f.getAbsolutePath() + ". This should never occur.");
                }
            }
            if (tsItem.length() > 0) {
                if (!f.getName().equals(this.transactionLogFilePath.getName())) {
                    throw new TransactionLogException("Last TransactionLog item in " + f.getAbsolutePath() + " is corrupted. This should never occur.");
                } else if (!this.committingFilePath.exists()) {
                    throw new TransactionLogException("Last TransactionLog item is incomplete while not in the committing state. This should never occur.");
                }
            }
            if (count > 0) {
                persistTripleStore(f);
            }
            totalCount += count;
            blr.close();
        }
        if (this.committingFilePath.exists()) {
            deleteFile(this.committingFilePath);
        }
        System.out.println("Recovering of " + totalCount + " items completed.");
        return;
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
}
