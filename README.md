# RDF Library

RDF Library bridges OWL API ontologies and the Sesame RDF model for the Protégé SPARQL Query plugin. Version 4 targets OWL API 5.5.1 and Java 11 for Protégé 6.

The public `org.protege.owl.rdf` API and Sesame 2.7.16 packages are retained. The bundle uses the host OWL API, logging, and Commons IO packages. Sesame's JAXB converter helpers and Activation implementation are private bundle dependencies; Protégé core does not need JAXB.

Build and run the translation, round-trip, surrogate identity, and parser regression tests with:

```sh
mvn --batch-mode clean verify
```

For a paired development build, install this snapshot with `mvn --batch-mode clean install`, then build the `protegeproject/sparql-query-plugin` Protégé 6 migration branch. Install both resulting bundle JARs in an assembled Protégé 6 distribution for interactive verification. This branch is tracked by protegeproject/protege#1357; the SPARQL migration is tracked by #1358.
