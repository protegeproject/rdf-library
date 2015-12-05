package org.protege.owl.rdf;

import info.aduna.iteration.CloseableIteration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class PizzaTests {
	public static Logger LOGGER = LoggerFactory.getLogger(PizzaTests.class);
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
	    ots = Utilities.getOwlTripleStore(ontology, false);
	}

	@Test
	public void testHasAxiom() throws RepositoryException, OWLOntologyCreationException {
		long startTime = System.currentTimeMillis();
		for (OWLAxiom axiom : ontology.getAxioms()) {
			Assert.assertTrue(ots.hasAxiom(ontology.getOntologyID(), axiom));
		}
		LOGGER.info("Parsing all the axioms from the triple store took " + (System.currentTimeMillis() - startTime) + "ms.");
	}
	
	@Test
	public void testListAxioms() throws RepositoryException, OWLOntologyCreationException {
		CloseableIteration<OWLAxiom, RepositoryException> axiomIt = ots.listAxioms(ontology.getOntologyID());
		Set<OWLAxiom> collected = new HashSet<OWLAxiom>();
		try {
			while (axiomIt.hasNext()) {
				OWLAxiom axiom = axiomIt.next();
				Assert.assertTrue(ontology.containsAxiom(axiom));
				collected.add(axiom);
			}
		}
		finally {
			axiomIt.close();
		}
		Assert.assertEquals(collected.size(), ontology.getAxiomCount());
	}
	
	@Test
	public void testRemove() throws RepositoryException {
		OWLAxiom axiom = selectInterestingAxiom();
		Assert.assertTrue(ots.hasAxiom(ontology.getOntologyID(), axiom));
		ots.removeAxiom(ontology.getOntologyID(), axiom);
		Assert.assertFalse(ots.hasAxiom(ontology.getOntologyID(), axiom));
		ots.removeAxiom(ontology.getOntologyID(), axiom);
        Assert.assertFalse(ots.hasAxiom(ontology.getOntologyID(), axiom));
	}
	
	@Test
	public void testRemoveNotPresent() throws RepositoryException {
        OWLAxiom present = selectInterestingAxiom();	    
        OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLClassExpression someValuesFrom = factory.getOWLObjectSomeValuesFrom(HAS_TOPPING, TOMATO_TOPPING);
        OWLClassExpression definition = factory.getOWLObjectIntersectionOf(PIZZA, someValuesFrom);
        OWLAxiom notPresent = factory.getOWLEquivalentClassesAxiom(CHEESEY_PIZZA, definition);

        Assert.assertFalse(ontology.containsAxiom(notPresent));
        Assert.assertTrue(ontology.containsAxiom(present));
        Assert.assertFalse(ots.hasAxiom(ontology.getOntologyID(), notPresent));
        Assert.assertTrue(ots.hasAxiom(ontology.getOntologyID(), present));
        
        ots.removeAxiom(ontology.getOntologyID(), notPresent);  // this used to remove everything...
        
        Assert.assertFalse(ots.hasAxiom(ontology.getOntologyID(), notPresent));
        Assert.assertTrue(ots.hasAxiom(ontology.getOntologyID(), present));
	}
	
	@Test
	public void testAdd() throws RepositoryException {
	    OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
	    OWLClassExpression someValuesFrom = factory.getOWLObjectSomeValuesFrom(HAS_TOPPING, TOMATO_TOPPING);
	    OWLClassExpression definition = factory.getOWLObjectIntersectionOf(PIZZA, someValuesFrom);
	    OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(CHEESEY_PIZZA, definition);
	    Assert.assertFalse(ontology.containsAxiom(axiom));
	    Assert.assertFalse(ots.hasAxiom(ontology.getOntologyID(), axiom));
	    ots.addAxiom(ontology.getOntologyID(), axiom);
	    Assert.assertTrue(ots.hasAxiom(ontology.getOntologyID(), axiom));
	}
	
	protected OWLAxiom selectInterestingAxiom() {
		OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		OWLClass cheeseyPizza = factory.getOWLClass(IRI.create(NS + "#CheeseyPizza"));
		return ontology.getEquivalentClassesAxioms(cheeseyPizza).iterator().next();
	}


}
