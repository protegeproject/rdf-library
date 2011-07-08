package org.protege.owl.rdf.api;

import info.aduna.iteration.CloseableIteration;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.semanticweb.owlapi.model.OWLAxiom;

public interface OwlTripleStore {

	Repository getRepository();
	
	void addAxiom(OWLAxiom axiom) throws RepositoryException;
	
	void removeAxiom(OWLAxiom axiom) throws RepositoryException;
	
	boolean hasAxiom(OWLAxiom axiom) throws RepositoryException;
	
	CloseableIteration<OWLAxiom, RepositoryException> listAxioms() throws RepositoryException;
	
	boolean integrityCheck() throws RepositoryException;
	
	boolean incorporateExternalChanges() throws RepositoryException;
}
