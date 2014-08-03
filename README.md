# Arango-cljs

ClojureScript interface to Foxx - the web framework runs inside ArangoDB
with full access to the data that is lightning fast.


**This is an alpha release. The API and organizational structure are
subject to change. Comments and contributions are much appreciated.**


# Features

- [x] Interface to Arango collections utilizing Prismatic's excellent
[Schema](https://github.com/Prismatic/schema) library to validate and coerce
between Clojure (in application context) and Javascript (to store in database).

- [x] A [Compojure](https://github.com/weavejester/compojure/)-inspired syntax
to define routes with ease.

- [x] A simple authentication middleware that is compatible with that of Foxx.

- [ ] Macros to automatically generate both API server/client libraries based on
collection data types' schemas.

Please see [Wiki](https://github.com/arango-cljs/arango-cljs/wiki)
and [sample application](sample-app/dev/app.cljs) for details.

## License

Copyright © 2014 Hoang Minh Thang

Distributed under the Eclipse Public License, the same as Clojure.
