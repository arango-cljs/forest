(ns arango.fs
  "File system utilities for Foxx applications"
  (:refer-clojure :exclude [parents]))

(def ^{:no-doc true :private true} fs (js/require "fs"))

(def ^{:no-doc true :private true} fs-path (js/require "path"))

(def ^{:doc "The platform-specific file separator,
  '\\' on Windows or '/' on *nix. "}
  separator (.-pathSeparator fs))

(def ^{:doc "The platform-specific path delimiter,
  ';' on Windows or ':' on *nix. "}
  delimiter (.-delimiter fs-path))

(defn exist?
  "Returns true if path exists."
  [path]
  (.exists fs path))

(defn directory? [path]
  (.isDirectory fs path))

(defn file? [path]
  (.isFile fs path))

(defn list-dir
  "Lists files and directories under path."
  [path]
  (.list fs path))

(defn list-tree
  "Lists all files and directories under path."
  [path]
  (.listTree fs path))

(defn move
  "Moves a file from `source` to `dest`."
  [source dest]
  (.move fs source dest))

(defn delete-file
  "Deletes a file."
  [path]
  (.remove fs path))

(defn delete-dir
  "Deletes an empty diretory"
  [directory]
  (.removeDirectory fs directory))

(defn delete
  "Deletes a file or an empty directory."
  [path]
  (if (file? path)
    (delete-file path)
    (delete-dir path)))

(defn delete-dir
  "Deletes a diretory recursively."
  [directory]
  (.removeDirectoryRecursive fs directory))

(defn spit
  "Opens a file, writes content, then closes it."
  [filename content]
  (.write fs filename content))

(defn slurp
  "Opens a file and reads all its contents, returning a string."
  [filename]
  (.read fs filename))

(defn slurp64
  "Like slurp but returns contents as a base64-encoded string."
  [filename]
  (.read64 fs filename))

(defn absolute
  "Returns absolute path."
  [path]
  (.makeAbsolute fs path))

(defn base-name
  "Returns the base name (final segment/file part) of a path.

  If optional `trim-ext` is a string and the path ends with that
  string, it is trimmed.

  If `trim-ext` is true, any extension is trimmed."
  ([path]
     (.basename fs-path path))
  ([path trim-ext]
     (if (string? trim-ext)
       (.basename fs-path path trim-ext)
       (let [base (.basename fs-path path)
             dot  (.lastIndexOf base ".")]
         (if (pos? dot) (.substring base 0 dot) base)))))

(comment
  (base-name "foo/bar") => "bar"
  (base-name "foo/bar.txt" true) => "bar"
  (base-name "bar.txt" ".txt") => "bar"
  (base-name "foo/bar.txt" ".png") => "bar.txt")

(defn extension
 "Returns the extension part of a file."
 [path]
 (let [ext (.extname fs-path path)]
   (when (seq ext) ext)))

(comment
  (fact (extension ?file) => ?ext)
  ?file ?ext
  "fs.clj" ".clj"
  "fs." "."
  "fs.clj.bak" ".bak"
  "/path/to/fs" nil
  "" nil
  ".bashrc" nil)

(def ^{:arglists ([& paths])
       :doc "Join all arguments together and normalize the resulting path.

  Arguments must be strings."}
  join
  (.-join fs-path))

(comment
  (join "a" "b/c" ".." "d") => "a/b/d")

(defn parent
  "Returns the parent path."
  [path]
  (when-not (= "/" path)
    (.dirname fs-path path)))

(defn parents
  "Gets all the parent directories of a path."
  [path]
  (when-let [parent (parent path)]
    (cons parent (lazy-seq (parents parent)))))

(defn normalize
  "Normalizes a string path, taking care of '..' and '.' parts.

  When multiple slashes are found, they're replaced by a single one;
  when the path contains a trailing slash, it is preserved. On Windows
  backslashes are used."
  [path]
  (.normalize fs-path path))

(comment
  (normalize "/foo/bar//baz/asdf/quux/..") => "/foo/bar/baz/asdf")

(defn size
  "Returns size (in bytes) of file."
  [path]
  (.size fs path))

(defn mkdir
  "Creates a directory."
  [path]
  (.makeDirectory fs path))

(defn mkdirs
  "Makes directory tree."
  [path]
  (.makeDirectoryRecursive fs path))

(defn temp-file
  "Creates a temporary file."
  ([]
     (.getTempFile fs "" true))
  ([dir]
     (.getTempFile fs dir true)))

(defn temp-name
  "Generates a temporary file name. Does not create file."
  ([]
     (.getTempFile fs))
  ([dir]
     (.getTempFile fs dir)))

(defn tmpdir
  "The temporary file directory."
  []
  (.getTempPath fs))

(defn home
  "Returns current user's home directory"
  []
  (.home fs))

(defn zip
  "Compresses files as a ZIP archive"
  ([filename chdir files]
   (.zipFile fs filename (or chdir "") (apply array files)))
  ([filename chdir files password]
   (.zipFile fs filename (or chdir "") (apply array files) password)))

(defn unzip
  "Extracts compressed files in a ZIP archive"
  ([filename out-path skip-paths overwrite?]
   (.unzipFile fs filename (or out-path "") (apply array skip-paths) overwrite?))
  ([filename out-path skip-paths overwrite? password]
   (.unzipFile fs filename (or out-path "") (apply array skip-paths) overwrite? password)))
