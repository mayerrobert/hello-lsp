/*

Simplest and most inefficient language server possible for experimenting with emacs' eglot.
Reads requests from stdin, writes responses to stdout.


eval in emacs:

    (with-eval-after-load 'eglot
      (add-to-list 'eglot-server-programs
                   '(lisp-mode . ("C:/robert.nobak/apps/Java/X64/jdk-19/bin/java"
                                  "-cp" "c:/robert/scripts/json-20240205.jar"
                                  "c:/robert/hello-lsp/src/main/java/Lsp.java"
                                  "--log" "c:/robert/lsp.log"
                                  ))))

then do "M-x eglot RET lisp-mode RET"


Sample "request" for running the "LSP" on the commandline:

    Content-Length: 50
    
    {"jsonrpc":"2.0","id":123,"method":"initialize"}

Do something like

    java -cp json-20240205.jar src/main/java/Lsp.java --log hello.log < lsp.req

to run the sample request and observe the log outputs.
An EOFException at the end is expected.

*/


import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Lsp {
    static final String CONTENT_LENGTH = "Content-Length";
    static final Pattern HEADER = Pattern.compile(CONTENT_LENGTH + ": ([0-9]+)");

    static PrintWriter log;

    private Lsp() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "--log".equals(args[0]))
            log = new PrintWriter(Files.newOutputStream(Paths.get(args[1])), true, StandardCharsets.UTF_8);
        else
            log = new PrintWriter(System.err, true);

        dbg("args: %s", String.join(", ", args));
        dbg("session start%n", null);
        try {
            Lsp.serve();
        }
        catch (Exception e) {
            e.printStackTrace(log);
        }
    }

    private static void serve() throws Exception {
        final Reader inReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        int id = -1;
        while (true) {
            dbg("begin request **************************%n", null);

            final int contentLength = readHeader(inReader);
            final String body = readChars(inReader, contentLength);
            dbg("received body '%s'", body);
            
            final var json = new JSONObject(body);
            dbg("body converted to json '%s'", json.toString(2));

            if (json.has("id")) id = json.getInt("id");
            else {
                dbg("client sent a request w/o id", null);
            }

            final String method = json.getString("method");
            dbg("parsed method=%s", method);

            switch (method) {
            case "initialize" -> initialize(id);
            case "textDocument/completion" -> completion(id);
            }
            
            dbg("done request ***************************%n", null);
        }
    }

    private static void initialize(int id) {
        respond(String.format("""
                              {
                                "jsonrpc": "2.0",
                                "id": %d,
                                "result": {
                                  "capabilities": {
                                    "completionProvider": {}
                                  }
                                }
                              }""", id));
    }

    private static void completion(int id) {
        respond(String.format("""
                              {
                                "jsonrpc": "2.0",
                                "id":%d
                                "result": {
                                  "isIncomplete": false,
                                  "items": [
                                    { "label": "a sample completion item" },
                                    { "label": "another sample completion item" }
                                  ]
                                }
                              }""", id));
    }


    /** first line coming in must be "Content-Length: ...", then consume and ignore header lines until receiving an empty line.
     *  Lines must be terminated by "\r\n". */
    private static int readHeader(Reader inReader) throws IOException {
        final String next = nextLine(inReader);
        dbg("received line '%s'%n", next);
        final Matcher matcher = HEADER.matcher(next);
        if (!matcher.find()) { throw new RuntimeException(CONTENT_LENGTH + " not found"); }

        String ignore;
        do {
            ignore = nextLine(inReader);
            if (ignore.isEmpty()) dbg("received empty line aka header ends", null);
            else dbg("read and ignored line '%s'", ignore);
        } while (!ignore.isEmpty());

        final int contentLength = Integer.parseInt(matcher.group(1));
        dbg("parsed " + CONTENT_LENGTH + ": %d%n", contentLength);
        return contentLength;
    }

    /** send {@code body} prefixed with a "Content-Length" header */
    private static void respond(String body) {
        final String s = String.format(CONTENT_LENGTH + ": %d\r\n\r\n%s", body.length(), body);
        dbg("sending esponse: '%s'%n", s);
        System.out.println(s);
    }


    private static void dbg(String fmt, Object arg) {
        if (log != null) log.format(fmt + "%n", arg);
    }

    private static String nextLine(Reader in) throws IOException {
        dbg("try to read a line", null);

        final StringBuilder ret = new StringBuilder();
        while (true) {
            int c = in.read();
            switch (c) {
            case -1 -> {
                dbg("EOF found after reading '%s'", ret.toString());
                throw new EOFException();
            }
            case '\r' -> {
                c = in.read();
                if (c != '\n') throw new IOException("CR not followed by LF");
                return ret.toString();
            }
            default -> ret.append((char)c);
            }
        }
    }

    private static String readChars(Reader reader, int n) throws Exception {
        dbg("try to read %d chars", n);
        final StringBuilder ret = new StringBuilder();
        while (n > 0) {
            ret.append((char)reader.read());
            --n;
        }
        return ret.toString();
    }
}
