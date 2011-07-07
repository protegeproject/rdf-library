package org.protege.owl.rdf.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.coode.owlapi.rdf.model.AbstractTranslator;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;


public class RDFTranslator extends AbstractTranslator<Value, Resource, org.openrdf.model.URI, org.openrdf.model.Literal> {
	private Repository repository;
	private org.openrdf.model.URI axiomResource;
	private Map<Object, BNode> bnodeMap = new HashMap<Object, BNode>();
	private RepositoryConnection connection;

	public RDFTranslator(Repository repository, OWLOntologyManager manager, OWLOntology ontology) {
		super(manager, ontology, true);
		this.repository = repository;
	}
	
	public org.openrdf.model.URI startTransaction() throws RepositoryException {
		ValueFactory factory = repository.getValueFactory();
		axiomResource = factory.createURI(OwlTripleStoreImpl.NS + "/" + UUID.randomUUID().toString().replace('-', '_'));
		bnodeMap.clear();
		connection = repository.getConnection();
		return axiomResource;
	}
	
	public void finishTransaction(boolean success) throws RepositoryException {
		if (success) {
			connection.commit();
		}
		else {
			connection.rollback();
		}
		connection.close();
	}
	
	public RepositoryConnection getConnection() {
		return connection;
	}

	@Override
	protected org.openrdf.model.URI getResourceNode(IRI iri) {
		ValueFactory factory = repository.getValueFactory();
		return factory.createURI(iri.toString());
	}

	@Override
	protected org.openrdf.model.URI getPredicateNode(IRI iri) {
		ValueFactory factory = repository.getValueFactory();
		return factory.createURI(iri.toString());
	}

	@Override
	protected BNode getAnonymousNode(Object key) {
		return repository.getValueFactory().createBNode();
	}

	@Override
	protected org.openrdf.model.Literal getLiteralNode(OWLLiteral literal) {
		ValueFactory factory = repository.getValueFactory();
		if (literal.isRDFPlainLiteral() && literal.getLang() != null) {
			return factory.createLiteral(literal.getLiteral(), literal.getLang());
		}
		else if (literal.isRDFPlainLiteral()) {
			return factory.createLiteral(literal.getLiteral());
		}
		else {
			return factory.createLiteral(literal.getLiteral(), factory.createURI(literal.getDatatype().getIRI().toString()));
		}
	}

	@Override
	protected void addTriple(Resource subject, org.openrdf.model.URI pred, Value object) {
		try {
			connection.add(subject, pred, object, axiomResource);
		}
		catch (RepositoryException e) {
			throw new RepositoryRuntimeException(e);
		}
	}

}
