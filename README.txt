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

    Content-Length: 48
    
    {"jsonrpc":"2.0","id":123,"method":"initialize"}

Do something like

    java -cp json-20240205.jar src/main/java/Lsp.java --log hello.log < lsp.req

to run the sample request and observe the log outputs.
An EOFException at the end is expected.
