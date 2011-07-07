package org.protege.owl.rdf;

import java.io.File;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
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
import org.semanticweb.owlapi.vocab.Namespaces;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;


public class BasicTests {

	/**
	 * @param args
	 * @throws RepositoryException 
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) throws RepositoryException, OWLOntologyCreationException {
		Sail sailStack = new MemoryStore();
		Repository repository = new SailRepository(sailStack);
		repository.initialize();
		OwlTripleStore ots = new OwlTripleStoreImpl(repository);
		RepositoryConnection connection = repository.getConnection();
		try {
			connection.setNamespace("owl", Namespaces.OWL.toString());
			connection.setNamespace("rdf", Namespaces.RDF.toString());
			connection.setNamespace("rdfs", Namespaces.RDFS.toString());
			connection.setNamespace("pizza", "http://www.co-ode.org/ontologies/pizza/pizza.owl#");
		}
		finally {
			connection.close();
		}
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File("/home/tredmond/Shared/ontologies/simple/pizza.owl"));
		for (OWLAxiom axiom : ontology.getAxioms()) {
			ots.addAxiom(axiom);
		}
		System.out.println(repository.toString());
		
		OWLClass cheesyPizza = manager.getOWLDataFactory().getOWLClass(IRI.create("http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseyPizza"));
		for (OWLAxiom axiom : ontology.getReferencingAxioms(cheesyPizza)) {
			System.out.println("Axiom " + axiom + " in repository: " + ots.hasAxiom(axiom));
		}
	}

}
