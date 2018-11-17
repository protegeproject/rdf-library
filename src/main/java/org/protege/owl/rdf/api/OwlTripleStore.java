package org.protege.owl.rdf.api;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntologyID;

public interface OwlTripleStore {

	Repository getRepository();
	
	void addAxiom(OWLOntologyID ontologyId, OWLAxiom axiom) throws RepositoryException;
	
	void removeAxiom(OWLOntologyID ontologyId, OWLAxiom axiom) throws RepositoryException;
	
	boolean hasAxiom(OWLOntologyID ontologyId, OWLAxiom axiom) throws RepositoryException;
		
	CloseableIteration<OWLAxiom, RepositoryException> listAxioms(OWLOntologyID ontologyId) throws RepositoryException;
	
	OWLClassExpression parseClassExpression(BNode classExpressionNode) throws RepositoryException;
	
	boolean integrityCheck() throws RepositoryException;
	
	boolean incorporateExternalChanges() throws RepositoryException;
}
