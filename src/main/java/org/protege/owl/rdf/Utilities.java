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
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class Utilities {

	private Utilities() {
		
	}
	
	public static OwlTripleStore getOwlTripleStore(OWLDataFactory factory) throws RepositoryException {
		Sail sailStack = new MemoryStore();
		Repository repository = new SailRepository(sailStack);
		repository.initialize();
		return new OwlTripleStoreImpl(repository, factory);
	}
	
	public static OwlTripleStore getOwlTripleStore(OWLOntologyManager manager, boolean sync) throws RepositoryException {
		OwlTripleStore ots = getOwlTripleStore(manager.getOWLDataFactory());
		for (OWLOntology ontology : manager.getOntologies()) {
			loadOwlTripleStore(ots, ontology, false);
		}
		if (sync) {
			synchronize(ots, manager);
		}
		return ots;
	}
	
	public static OwlTripleStore getOwlTripleStore(OWLOntology ontology, boolean sync) throws RepositoryException {
		OwlTripleStore ots = getOwlTripleStore(ontology.getOWLOntologyManager().getOWLDataFactory());
		loadOwlTripleStore(ots, ontology, sync);
		return ots;
	}

	public static void loadOwlTripleStore(OwlTripleStore ots, OWLOntology ontology, boolean sync) throws RepositoryException {
	   ots.addAxioms(ontology.getOntologyID(), ontology.getAxioms());
		if (sync) {
			synchronize(ots, ontology.getOWLOntologyManager());
		}
	}
	
	public static void synchronize(OwlTripleStore ots, OWLOntologyManager manager) {
		manager.addOntologyChangeListener(new SynchronizeTripleStoreListener(ots));
	}
	
}
