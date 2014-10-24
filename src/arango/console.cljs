(ns arango.console)

(def ^{:no-doc true :private true}
  console (js/require "console"))

(def ^{:arglists ([format & args])
       :doc "Formats the arguments according to format and logs the
  result as an info message.

  String substitution patterns, which can be used in `format` argument
  of `info`, `error` and `warn`.

   - `%%s`: string
   - `%%d`, `%%i`: integer
   - `%%f`: floating point number
   - `%%o`: object hyperlink"}
  info (.-info console))

(def ^{:arglists ([format & args])
       :doc "Formats the arguments according to format and logs the
  result as an error message."}
  error (.-error console))

(def ^{:arglists ([format & args])
       :doc "Formats the arguments according to format and logs the
  result as a warning message."}
  warn (.-warn console))

(def ^{:arglists ([timer-name])
       :doc "Creates a new timer under the given name. Call time-end
  with the same name to stop the timer and log the time elapsed."}
  time (.-time console))

(def ^{:arglists ([timer-name])
       :doc "Stops a timer created by a call to time and logs the time elapsed."}
  time-end (.-timeEnd console))

(def ^{:arglists ([format & args])
       :doc "Formats the arguments according to format and logs the
  result as log message.

  Opens a nested block to indent all future messages sent. Call `group-end`
  to close the block. Representation of block is up to the platform, it
  can be an interactive block or just a set of indented sub messages."}
  group (.-group console))

(def ^{:arglists ([format & args])
       :doc "Stops a timer created by a call to time and logs the time elapsed."}
  group-end (.-groupEnd console))

(def ^{:arglists ([])
       :doc "Reads in a line from the console and returns it as string."}
  read-line (.-getline console))
