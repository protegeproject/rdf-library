package org.protege.owl.rdf;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;
import org.protege.owl.rdf.api.OwlTripleStore;
import org.protege.owl.rdf.impl.OwlTripleStoreImpl;
import org.protege.owl.rdf.impl.SynchronizeTripleStoreListener;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class Utilities {

	private Utilities() {
		
	}
	
	public static OwlTripleStore getOwlTripleStore(OWLOntology ontology, boolean sync) throws RepositoryException {
		Sail sailStack = new MemoryStore();
		Repository repository = new SailRepository(sailStack);
		repository.initialize();
		return loadRepository(repository, ontology, sync);
	}
	
	public static OwlTripleStore loadRepository(Repository repository, OWLOntology ontology, boolean sync) throws RepositoryException {
		OwlTripleStore ots = new OwlTripleStoreImpl(repository, ontology.getOWLOntologyManager().getOWLDataFactory());
		loadOwlTripleStore(ots, ontology, sync);
		return ots;
	}
	
	public static void loadOwlTripleStore(OwlTripleStore ots, OWLOntology ontology, boolean sync) throws RepositoryException {
		for (OWLAxiom axiom : ontology.getAxioms()) {
			ots.addAxiom(ontology.getOntologyID(), axiom);
		}
		if (sync) {
			OWLOntologyManager manager = ontology.getOWLOntologyManager();
			manager.addOntologyChangeListener(new SynchronizeTripleStoreListener(ots));
		}
	}
	
	
}
