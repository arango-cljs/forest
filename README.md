# "For REST" - a cozy home for (ArangoDB) Foxx applications.

Write scalable, database-ready APIs and apps in Clojurescript with ease.


**This is an alpha release. The API and organizational structure are
subject to change. Comments and contributions are much appreciated.**


# Features

- [x] Interface to Arango collections utilizing Prismatic's excellent
[Schema](https://github.com/Prismatic/schema) library to validate and coerce
between Clojure (in application context) and Javascript (to store in database).

- [x] A [Compojure](https://github.com/weavejester/compojure/)-inspired syntax
to define routes with ease.

# Todos

- [ ] Helper for rendering [Om](https://github.com/swannodette/om) components on server side.

- [ ] Utilize [transducers](http://clojure.org/transducers) in Forest's collection API.

- [ ] Implement full [Ring SPEC](https://github.com/mmcgrana/ring/blob/master/SPEC)

Please visit [API](https://arango-cljs.github.io/forest/0.2.0/index.html)
and have a look at the [sample application](sample-app/dev/app.cljs) for details.

## License

Copyright © 2014 Hoang Minh Thang

Distributed under the Eclipse Public License, the same as Clojure.
