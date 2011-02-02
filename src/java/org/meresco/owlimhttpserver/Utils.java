package org.meresco.owlimhttpserver;

import java.util.HashMap;
import java.util.ArrayList;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.IOException;


class Utils {
    public static QueryParameters parseQS(String queryString) {
        /*
         * shamelessly copied from: http://stackoverflow.com/questions/1667278/parsing-queryString-strings-in-java
         */
        QueryParameters params = new QueryParameters();
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

}

