package com.franz.jig;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class JigScriptEngine implements ScriptEngine {
    private static final String[] steps = new String[]{
            "_",
            "bothE",
            "common",
            "distinct",
            "E",
            "ends",
            "head",
            "id",
            "inE",
            "label",
            "limit",
            "nearby",
            "out",
            "outE",
            "tail",
            "triples",
            "V"};
    private static final String[] methods = new String[]{
            "aggr",
            "count",
            "eval",
            "mean",
            "path",
            "sum",
    };

    private static final Collection<String> keywords;

    static {
        keywords = new LinkedList<String>();
        keywords.addAll(Arrays.asList(steps));
        keywords.addAll(Arrays.asList(methods));
    }

    private final String endpoint;
    private final String userName;
    private final String password;
    private final HttpClient client;

    public JigScriptEngine(final String endpoint,
                           final String userName,
                           final String password) {
        this.endpoint = endpoint;
        this.userName = userName;
        this.password = password;
        //System.out.println("using credentials: (" + userName + ", " + password + ")");
        client = createHttpClient();
    }

    public Object eval(final String script,
                       final ScriptContext context) throws ScriptException {
        // TODO
        return null;
    }

    public Object eval(final Reader reader,
                       final ScriptContext context) throws ScriptException {
        // TODO
        return null;
    }

    public Object eval(final String script) throws ScriptException {
        try {
            return issueRequest(transformScript(script));
        } catch (IOException e) {
            throw new ScriptException(e);
        } catch (JSONException e) {
            throw new ScriptException(e);
        }
    }

    private String transformScript(final String script) {
        // Emulate method metaprogramming at a syntactic level (since the JavaScript-to-Lisp compiler doesn't support it)
        String r = script.trim();
        while (r.endsWith(";")) {
            r = r.substring(0, r.length() - 1).trim();
        }
        for (String k : keywords) {
            r = r.replaceAll("[.]" + k + "[.]", "." + k + "().");
            r = r.replaceAll("[.]" + k + "\\[", "." + k + "()[");
            r = r.replaceAll("[.]" + k + "$", "." + k + "()");
            //r = r.replaceAll("[.]" + k + "\\s*;", "." + k + "();");
        }
        r = cleanParens(r);
        r = cleanBrackets(r);
        for (String step : steps) {
            if (r.endsWith("." + step)) {
                r = r + ".eval()";
                break;
            }
        }

        //r = "turtlefy(" + r + ")";
        //System.out.println("[ r = " + r + "]");
        return r;
    }

    // appends an ".eval()" for raw steps
    // e.g. "foo.head()" becomes "foo.head().eval()"
    // note: this is an expensive operation
    private String cleanParens(final String r) {
        if (r.endsWith(")")) {
            int i = r.lastIndexOf("(");
            String s = r.substring(0, i);
            for (String step : steps) {
                if (s.endsWith("." + step)) {
                    return r + ".eval()";
                }
            }
        }

        return r;
    }

    private String cleanBrackets(final String r) {
        if (r.endsWith("]")) {
            int i = r.lastIndexOf("[");
            return cleanParens(r.substring(0, i)) + r.substring(i);
        } else {
            return r;
        }
    }

    public Object eval(final Reader reader) throws ScriptException {
        // TODO
        return null;
    }

    public Object eval(final String script,
                       final Bindings bindings) throws ScriptException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object eval(final Reader reader,
                       final Bindings bindings) throws ScriptException {
        // TODO
        return null;
    }

    public void put(final String key,
                    final Object value) {
        // TODO
    }

    public Object get(final String key) {
        // TODO
        return null;
    }

    public Bindings getBindings(final int scope) {
        // TODO
        return null;
    }

    public void setBindings(final Bindings bindings,
                            final int i) {
        // TODO
    }

    public Bindings createBindings() {
        // TODO
        return null;
    }

    public ScriptContext getContext() {
        // TODO
        return null;
    }

    public void setContext(final ScriptContext context) {
        // TODO
    }

    public ScriptEngineFactory getFactory() {
        // TODO
        return null;
    }

    private static HttpClient createHttpClient() {
        // TODO: additional settings, e.g. agent identity
        return new DefaultHttpClient();
    }

    private Object issueRequest(final String script) throws IOException, JSONException {
        HttpPost request = new HttpPost(endpoint);
        request.setHeader("Content-Type", "text/javascript");
        request.setEntity(new StringEntity(script));
        String auth = new String(Base64.encodeBase64((userName + ":" + password).getBytes()));
        request.setHeader("Authorization", "Basic " + auth);

        //long before = new Date().getTime();
        HttpResponse response = client.execute(request);
        long after = new Date().getTime();
        //System.out.println("[request took " + (after - before) + "ms]");
        int responseCode = response.getStatusLine().getStatusCode();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        response.getEntity().writeTo(bos);
        String s = bos.toString();
        //System.out.println("here is the result: " + s);
        //System.out.println("response: " + responseCode);

        return interpretResponse(s.trim());
    }

    private Object interpretResponse(final String s) throws JSONException {
        if (s.startsWith("[")) {
            return new JSONArray(s);
        } else if (s.startsWith("{")) {
            return new JSONObject(s);
        } else {
            //JSONArray a = new JSONArray("[\"" + s + "\"]");
            //return a.get(0);
            return s;
        }
    }

    public void initialize() throws ScriptException {
        InputStream is = Jig.class.getResourceAsStream("jig.js");
        final String s;
        try {
            try {
                s = convertStreamToString(is);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw new ScriptException(e);
        }

        eval(s);
    }

    private String convertStreamToString(final InputStream is)
            throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }
}
