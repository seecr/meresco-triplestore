/* begin license *
 * 
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server. 
 * 
 * Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
 * Copyright (C) 2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
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

import java.util.HashMap;
import java.util.ArrayList;

import java.net.URLDecoder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

class Utils {
    public static QueryParameters parseQS(String queryString) {
        /*
         * shamelessly copied from: http://stackoverflow.com/questions/1667278/parsing-queryString-strings-in-java
         */
        QueryParameters params = new QueryParameters();
        if (queryString == null) {
            return params;
        }

        for (String param : queryString.split("&")) {
            if (param.indexOf('=') > 0) {
                String[] pair = param.split("=");
                try {
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = URLDecoder.decode(pair[1], "UTF-8");
                    ArrayList<String> values = params.get(key);
                    if (values == null) {
                        values = new ArrayList<String>();
                        params.put(key, values);
                    }
                    values.add(value);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return params;
    }


    public static File createTempDirectory() throws IOException {
        final File temp;
        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        temp.deleteOnExit();
        return temp;
    }

    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i=0; i<files.length; i++) {
                if (files[i].isDirectory()) {
                   deleteDirectory(files[i]);
                } else {
                   files[i].delete();
                }
            }
        }
        return path.delete();
    }

    public void pause() {
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String read(File f) throws IOException { 
        BufferedReader br = new BufferedReader(new FileReader(f));
        String data = "";
        String line;
        while ((line = br.readLine()) != null) {
            data += line;
        }
        return data;
    }
    
    public static String read(InputStream in) {
    	StringBuilder contents = new StringBuilder();
    	
    	BufferedReader input = null;
        try { 
            try {
                input =  new BufferedReader(new InputStreamReader(in));
                String line = null;
                while (( line = input.readLine()) != null) {
                	System.out.println(">>>>"+line);
                  contents.append(line);
                  contents.append(System.getProperty("line.separator"));
                }
            }
            finally {
                input.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return contents.toString().trim();
    }

    public static String getStackTrace(Throwable aThrowable) {
        /* 
         * shameless partial copy from: 
         * http://www.javapractices.com/topic/TopicAction.do?Id=78 
         */
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

}
