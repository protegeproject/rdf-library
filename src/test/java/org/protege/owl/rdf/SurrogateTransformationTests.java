package org.protege.owl.rdf;

import org.protege.owl.rdf.impl.AnonymousResourceHandler;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.testng.annotations.Test;
import org.openrdf.model.Statement;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.rdfxml.RDFXMLParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SurrogateTransformationTests {
    private final OWLDataFactory df = OWLManager.getOWLDataFactory();

    @Test
    public void preservesNestedAnnotationsAndAnonymousIndividuals() {
        OWLAnonymousIndividual individual = df.getOWLAnonymousIndividual();
        OWLAnnotation nested = df.getOWLAnnotation(df.getRDFSComment(), individual);
        OWLAnnotation annotation = df.getOWLAnnotation(df.getRDFSLabel(), individual,
                Collections.singleton(nested));
        OWLAxiom axiom = df.getOWLClassAssertionAxiom(df.getOWLThing(), individual,
                Collections.singleton(annotation));
        AnonymousResourceHandler handler = new AnonymousResourceHandler(df);
        OWLAxiom stored = handler.insertSurrogates(axiom);
        assertTrue(stored.getAnonymousIndividuals().isEmpty());
        assertEquals(handler.removeSurrogates(stored), axiom);
    }

    @Test
    public void preservesAnonymousAnnotationSubjectsAndValues() {
        OWLAnonymousIndividual subject = df.getOWLAnonymousIndividual();
        OWLAnonymousIndividual value = df.getOWLAnonymousIndividual();
        OWLAxiom axiom = df.getOWLAnnotationAssertionAxiom(df.getRDFSComment(), subject, value,
                Collections.singleton(df.getOWLAnnotation(df.getRDFSLabel(), value)));
        AnonymousResourceHandler handler = new AnonymousResourceHandler(df);
        OWLAxiom stored = handler.insertSurrogates(axiom);
        assertTrue(stored.getAnonymousIndividuals().isEmpty());
        assertEquals(handler.removeSurrogates(stored), axiom);
    }

    @Test
    public void preservesDatatypeFacetsWhileTransformingIndividuals() {
        OWLDataProperty property = df.getOWLDataProperty(IRI.create("urn:test#age"));
        OWLDataRange range = df.getOWLDatatypeRestriction(df.getIntegerOWLDatatype(),
                df.getOWLFacetRestriction(OWLFacet.MIN_INCLUSIVE, df.getOWLLiteral(18)));
        OWLAxiom axiom = df.getOWLClassAssertionAxiom(df.getOWLDataSomeValuesFrom(property, range),
                df.getOWLAnonymousIndividual());
        AnonymousResourceHandler handler = new AnonymousResourceHandler(df);
        assertEquals(handler.removeSurrogates(handler.insertSurrogates(axiom)), axiom);
    }

    @Test
    public void parsesLongBlankNodeIdentifiersOnJava11() throws Exception {
        String nodeId = "node" + "x".repeat(256);
        String rdf = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
                + "xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">"
                + "<rdf:Description rdf:nodeID=\"" + nodeId + "\">"
                + "<rdfs:label>Example</rdfs:label><rdfs:comment>Same node</rdfs:comment>"
                + "</rdf:Description></rdf:RDF>";
        List<Statement> statements = new ArrayList<>();
        RDFXMLParser parser = new RDFXMLParser();
        parser.setRDFHandler(new RDFHandlerBase() {
            @Override
            public void handleStatement(Statement statement) {
                statements.add(statement);
            }
        });
        parser.parse(new StringReader(rdf), "urn:test:");
        assertEquals(statements.size(), 2);
        assertEquals(statements.get(0).getSubject(), statements.get(1).getSubject());
    }
}
