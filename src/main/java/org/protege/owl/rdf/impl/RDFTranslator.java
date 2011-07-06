package org.protege.owl.rdf.impl;

import org.coode.owlapi.rdf.model.AbstractTranslator;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;


public class RDFTranslator extends AbstractTranslator<Value, Resource, URI, Literal> {

	public RDFTranslator(OWLOntologyManager manager, OWLOntology ontology, boolean useStrongTyping) {
		super(manager, ontology, useStrongTyping);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Resource getResourceNode(IRI IRI) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	protected URI getPredicateNode(IRI IRI) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	protected Resource getAnonymousNode(Object key) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	protected Literal getLiteralNode(OWLLiteral literal) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	protected void addTriple(Resource subject, URI pred, Value object) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

}
