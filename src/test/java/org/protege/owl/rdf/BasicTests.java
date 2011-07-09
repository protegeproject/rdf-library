package org.protege.owl.rdf;

import info.aduna.iteration.CloseableIteration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;
import org.protege.owl.rdf.api.OwlTripleStore;
import org.protege.owl.rdf.impl.OwlTripleStoreImpl;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


public class BasicTests {
	public static Logger LOGGER = Logger.getLogger(BasicTests.class);
	public static String NS = "http://www.co-ode.org/ontologies/pizza/pizza.owl";
	
	private OWLOntology ontology;
	private OwlTripleStore ots;
	
	@BeforeMethod
	public void setup() throws OWLOntologyCreationException, RepositoryException {
		ontology = getOntology();
		loadTripleStore();
	}

	@Test
	public void testRead() throws RepositoryException, OWLOntologyCreationException {
		long startTime = System.currentTimeMillis();
		for (OWLAxiom axiom : ontology.getAxioms()) {
			Assert.assertTrue(ots.hasAxiom(axiom));
		}
		LOGGER.info("Parsing all the axioms from the triple store took " + (System.currentTimeMillis() - startTime) + "ms.");
	
		CloseableIteration<OWLAxiom, RepositoryException> axiomIt = ots.listAxioms();
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
		Assert.assertTrue(ots.hasAxiom(axiom));
		ots.removeAxiom(axiom);
		Assert.assertFalse(ots.hasAxiom(axiom));
	}
	
	
	protected OWLOntology getOntology() throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		return manager.loadOntologyFromOntologyDocument(new File("src/test/resources/pizza.owl"));
	}
	
	protected OWLAxiom selectInterestingAxiom() {
		OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		OWLClass cheeseyPizza = factory.getOWLClass(IRI.create(NS + "#CheeseyPizza"));
		return ontology.getEquivalentClassesAxioms(cheeseyPizza).iterator().next();
	}
	
	private void loadTripleStore() throws RepositoryException {
		Sail sailStack = new MemoryStore();
		Repository repository = new SailRepository(sailStack);
		repository.initialize();
		ots = new OwlTripleStoreImpl(repository);
		

		long startTime = System.currentTimeMillis();
		for (OWLAxiom axiom : ontology.getAxioms()) {
			ots.addAxiom(axiom);
		}
		LOGGER.info("Loading the pizza into the repository took " + (System.currentTimeMillis() - startTime) + "ms.");
	}

}
