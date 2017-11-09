package org.protege.owl.rdf.api;

import info.aduna.iteration.CloseableIteration;
import java.util.Set;
import org.openrdf.model.BNode;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntologyID;

public interface OwlTripleStore {

	Repository getRepository();
	
	void addAxiom(OWLOntologyID ontologyId, OWLAxiom axiom) throws RepositoryException;
	
    void addAxioms(OWLOntologyID ontologyId, Set<OWLAxiom> axiom) throws RepositoryException;
        
	void removeAxiom(OWLOntologyID ontologyId, OWLAxiom axiom) throws RepositoryException;
	
	boolean hasAxiom(OWLOntologyID ontologyId, OWLAxiom axiom) throws RepositoryException;
		
	CloseableIteration<OWLAxiom, RepositoryException> listAxioms(OWLOntologyID ontologyId) throws RepositoryException;
	
	OWLClassExpression parseClassExpression(BNode classExpressionNode) throws RepositoryException;
	
	boolean integrityCheck() throws RepositoryException;
	
	boolean incorporateExternalChanges() throws RepositoryException;
}
