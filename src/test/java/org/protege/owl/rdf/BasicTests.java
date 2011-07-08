package org.protege.owl.rdf;

import info.aduna.iteration.CloseableIteration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

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
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.testng.Assert;
import org.testng.annotations.Test;


public class BasicTests {

	@Test
	public static void testReadPizza() throws RepositoryException, OWLOntologyCreationException {
		Sail sailStack = new MemoryStore();
		Repository repository = new SailRepository(sailStack);
		repository.initialize();
		OwlTripleStore ots = new OwlTripleStoreImpl(repository);
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File("src/test/resources/pizza.owl"));
		for (OWLAxiom axiom : ontology.getAxioms()) {
			ots.addAxiom(axiom);
		}
		
		for (OWLAxiom axiom : ontology.getAxioms()) {
			Assert.assertTrue(ots.hasAxiom(axiom));
		}
		
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

}
