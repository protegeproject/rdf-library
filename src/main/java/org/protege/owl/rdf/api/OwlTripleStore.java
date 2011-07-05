package org.protege.owl.rdf.api;

import org.openrdf.model.Resource;
import org.openrdf.repository.Repository;
import org.semanticweb.owlapi.model.OWLAxiom;

public interface OwlTripleStore {

	Repository getRepository();
	
	Resource addAxiom(OWLAxiom axiom);
	
	void removeAxiom(OWLAxiom axiom);
	
	
}
