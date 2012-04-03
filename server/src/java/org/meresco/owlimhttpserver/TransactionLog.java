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

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

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
    final static String CURRENT_TRANSACTIONLOG_NAME = "current";

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
        this.tempLogDir.mkdirs();
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
        ArrayList<String> sortedTsFiles = getTransactionItemFiles();
        long lastAddedTimeStamp = sortedTsFiles.size() > 1 ? Long.valueOf(sortedTsFiles.get(sortedTsFiles.size() - 2)) : 0;
        if (newFilename < lastAddedTimeStamp) { // in theory: only small differences by ntp 
            return;
        }
        try {
            this.transactionLog.close();
            File newFile = new File(this.transactionLogDir, String.valueOf(newFilename));
            renameFileTo(this.transactionLogFilePath, newFile);
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
        renameFileTo(this.committedFilePath, this.committingFilePath);
        commit_do(filename);
        renameFileTo(this.committingFilePath, this.committedFilePath);
    }

    void commit_do(String filename) throws IOException {
        File tmpFilepath = new File(this.tempLogDir, filename);
        BufferedLineReader blr = new BufferedLineReader(new FileReader(tmpFilepath));
        String line;
        while ((line = blr.readLine()) != null) {
            this.transactionLog.write(line.getBytes(Charset.defaultCharset()));
        }
        blr.close();
        deleteFile(tmpFilepath);
    }

    private void deleteFile(File tmpFilepath) throws IOException {
		if (!tmpFilepath.delete()) {
			throw new IOException("File could not be deleted.");
		}
	}

	void rollback(String filename) throws IOException {
		File tmpFile = new File(this.tempLogDir, filename);
		if (tmpFile.isFile()) {
			deleteFile(tmpFile);
		}
    }

    void rollbackAll(String filename, long originalPosition) throws IOException {
        this.tripleStore.undoCommit();
        this.transactionLog.setLength(originalPosition);
        renameFileTo(this.committingFilePath, this.committedFilePath);
        rollback(filename);
    }

    private void renameFileTo(File from, File to) throws IOException {
		if (!from.renameTo(to)) {
			throw new IOException("File " + from.getAbsolutePath() + " could not be moved to " + to.getAbsolutePath());
		}
	}

	File getTempLogDir() {
        return this.tempLogDir;
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
        String tsItem = "";
        for (File f : tsFiles) {
            int count = 0;
            BufferedLineReader blr = new BufferedLineReader(new FileReader(f));
            String line;
            while ((line = blr.readLine()) != null) {
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
                    throw new TransactionLogException("Corrupted transaction_item in " + f.getAbsolutePath() + ". This should never occur.");
                }
            }
            if (!tsItem.equals("")) {
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
}
