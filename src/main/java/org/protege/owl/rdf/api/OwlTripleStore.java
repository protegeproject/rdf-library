package org.protege.owl.rdf.api;

import org.openrdf.model.Resource;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

public interface OwlTripleStore {

	Repository getRepository();
	
	Resource addAxiom(OWLAxiom axiom) throws RepositoryException;
	
	void removeAxiom(OWLAxiom axiom) throws RepositoryException;
	
	boolean hasAxiom(OWLAxiom axiom) throws RepositoryException;
}
