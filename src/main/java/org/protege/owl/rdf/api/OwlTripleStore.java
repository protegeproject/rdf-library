package org.protege.owl.rdf.api;

import info.aduna.iteration.CloseableIteration;

import org.openrdf.model.Resource;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.semanticweb.owlapi.model.OWLAxiom;

public interface OwlTripleStore {

	Repository getRepository();
	
	Resource addAxiom(OWLAxiom axiom) throws RepositoryException;
	
	void removeAxiom(OWLAxiom axiom) throws RepositoryException;
	
	boolean hasAxiom(OWLAxiom axiom) throws RepositoryException;
	
	CloseableIteration<OWLAxiom, RepositoryException> listAxioms();
	
	boolean integrityCheck();
	
	boolean incorporateExternalChanges();
}
