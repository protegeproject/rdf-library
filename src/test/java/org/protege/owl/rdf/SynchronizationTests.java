package org.protege.owl.rdf;

import java.io.File;

import org.openrdf.repository.RepositoryException;
import org.protege.owl.rdf.api.OwlTripleStore;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SynchronizationTests {
    public static String NS = "http://www.co-ode.org/ontologies/pizza/pizza.owl";

    public static final OWLClass PIZZA          = OWLManager.getOWLDataFactory().getOWLClass(IRI.create(NS + "#Pizza"));
    public static final OWLClass CHEESEY_PIZZA  = OWLManager.getOWLDataFactory().getOWLClass(IRI.create(NS + "#CheeseyPizza"));
    public static final OWLClass TOMATO_TOPPING = OWLManager.getOWLDataFactory().getOWLClass(IRI.create(NS + "#TomatoTopping"));
    public static final OWLObjectProperty HAS_TOPPING = OWLManager.getOWLDataFactory().getOWLObjectProperty(IRI.create(NS + "#hasTopping"));
    
    private OWLOntology ontology;
    private OwlTripleStore ots;
    
    @BeforeMethod
    public void setup() throws OWLOntologyCreationException, RepositoryException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument(new File("src/test/resources/pizza.owl"));
        ots = Utilities.getOwlTripleStore(ontology, true);
    }
    
    @Test
    public void testAdd() throws RepositoryException {
        OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLClassExpression someValuesFrom = factory.getOWLObjectSomeValuesFrom(HAS_TOPPING, TOMATO_TOPPING);
        OWLClassExpression definition = factory.getOWLObjectIntersectionOf(PIZZA, someValuesFrom);
        OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(CHEESEY_PIZZA, definition);
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        manager.addAxiom(ontology, axiom);
        Assert.assertTrue(ots.hasAxiom(ontology.getOntologyID(), axiom));
    }
    
    @Test
    public void testRemove() throws RepositoryException {
        OWLAxiom axiom = selectInterestingAxiom();
        Assert.assertTrue(ots.hasAxiom(ontology.getOntologyID(), axiom));
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        manager.removeAxiom(ontology, axiom);
        Assert.assertFalse(ots.hasAxiom(ontology.getOntologyID(), axiom));
    }
    
    
    protected OWLAxiom selectInterestingAxiom() {
        OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLClass cheeseyPizza = factory.getOWLClass(IRI.create(NS + "#CheeseyPizza"));
        return ontology.getEquivalentClassesAxioms(cheeseyPizza).iterator().next();
    }
}
