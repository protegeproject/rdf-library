package org.protege.owl.rdf.impl;

import org.openrdf.model.Resource;
import org.openrdf.repository.Repository;
import org.protege.owl.rdf.api.OwlTripleStore;
import org.semanticweb.owlapi.model.OWLAxiom;

public class OwlTripleStoreImpl implements OwlTripleStore {

	@Override
	public Repository getRepository() {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public Resource addAxiom(OWLAxiom axiom) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void removeAxiom(OWLAxiom axiom) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

}
