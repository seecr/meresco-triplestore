package org.meresco.owlimhttpserver;

import java.util.HashMap;
import java.util.ArrayList;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;


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
}

