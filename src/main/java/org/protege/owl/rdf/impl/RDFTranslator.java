package org.protege.owl.rdf.impl;

import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.model.AbstractTranslator;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;


public class RDFTranslator extends AbstractTranslator<Value, Resource, org.openrdf.model.URI, org.openrdf.model.Literal> {
    public static final Logger LOGGER = LoggerFactory.getLogger(RDFTranslator.class);
	private org.openrdf.model.URI axiomResource;

	/**
	 * There is a dangerous bend coming up!  If you don't use the identity hash map then this 
	 * code doesn't work.  The identity hashmap is used to ensure that lists are not reused in 
	 * non-standard ways by different axioms.  (I think that this might actually lead to an OWL full by
	 * the specification because of how triples are "consumed" by the translator).  But there is
	 * an additional problem (not quite a bug because it works) in the AbstractTranslator.translateList
	 * method in an off by one error when it generates the key for getAnonymousNode.  So using a 
	 * HashMap here leads to unexpectedly bad results even when you would not expect that lists 
	 * would be shared.
	 */
	private Map<Object, BNode> bnodeMap = new IdentityHashMap<>();
	private static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	private ValueFactory rdfFactory;
	private RepositoryConnection connection;
	
	public static void translate(Repository repository, OWLAxiom axiom, 
	                             org.openrdf.model.URI hashCodeProperty,
	                             org.openrdf.model.URI sourceOntologyProperty,
	                             org.openrdf.model.URI ontologyRepresentative) throws RepositoryException {
	    if (LOGGER.isDebugEnabled()) {
	        LOGGER.debug("Starting axiom parse");
	    }
		boolean success = false;

		RDFTranslator translator = null;
		
		try {
			OWLOntology ontology = createOntology(manager, axiom);
		    translator = new RDFTranslator(repository, manager, ontology);
			ValueFactory rdfFactory = repository.getValueFactory();
			RepositoryConnection connection = translator.getConnection();
			axiom.accept(translator);
			for (OWLEntity entity : axiom.getSignature()) {  // why aren't these getting included?
				connection.add(rdfFactory.createURI(entity.getIRI().toString()), 
						       rdfFactory.createURI(OWLRDFVocabulary.RDF_TYPE.getIRI().toString()), 
						       rdfFactory.createURI(entity.getEntityType().getVocabulary().getIRI().toString()), 
						       translator.axiomResource);
			}

			org.openrdf.model.Literal hashCodeValue = rdfFactory.createLiteral(axiom.hashCode());
			connection.add(translator.axiomResource, hashCodeProperty, hashCodeValue);
			connection.add(translator.axiomResource, sourceOntologyProperty, ontologyRepresentative);
			success = true;
		}
		catch (RepositoryRuntimeException rre) {
			throw rre.getCause();
		} catch (OWLOntologyCreationException e) {
			throw new RepositoryException(e);
		}
		finally {
			if (translator != null) {
				translator.close(success);
			}
		}
		if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug("Finished axiom parse");
		}
	}

	private static OWLOntology createOntology(OWLOntologyManager manager, OWLAxiom axiom) throws OWLOntologyCreationException {
		Set<OWLAxiom> axiomSet = new HashSet<>();
		axiomSet.add(axiom);
		return manager.createOntology(axiomSet);
	}

	private RDFTranslator(Repository repository, OWLOntologyManager manager, OWLOntology ontology) throws RepositoryException {
		super(manager, ontology, null,false, i -> true, new HashSet<>());
		rdfFactory = repository.getValueFactory();
		axiomResource = rdfFactory.createURI(OwlTripleStoreImpl.NS + "/" + UUID.randomUUID().toString().replace('-', '_'));
		connection = repository.getConnection();
	}

	
	public void close(boolean success) throws RepositoryException {
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

	@Nonnull
	@Override
	protected org.openrdf.model.URI getResourceNode(@Nonnull IRI iri) {
		return rdfFactory.createURI(iri.toString());
	}

	@Nonnull
	@Override
	protected org.openrdf.model.URI getPredicateNode(@Nonnull IRI iri) {
		return rdfFactory.createURI(iri.toString());
	}

	@Nonnull
	@Override
	protected BNode getAnonymousNode(@Nonnull Object key) {
		BNode node = bnodeMap.get(key);
		if (node == null) {
			node = rdfFactory.createBNode();
			bnodeMap.put(key, node);
		}
		return node;
	}

	@Nonnull
	@Override
	protected org.openrdf.model.Literal getLiteralNode(@Nonnull OWLLiteral literal) {
		if (literal.isRDFPlainLiteral() && !literal.getLang().isEmpty()) {
			return rdfFactory.createLiteral(literal.getLiteral(), literal.getLang());
		}
		else if (literal.isRDFPlainLiteral()) {
			return rdfFactory.createLiteral(literal.getLiteral());
		}
		else {
			return rdfFactory.createLiteral(literal.getLiteral(), rdfFactory.createURI(literal.getDatatype().getIRI().toString()));
		}
	}

	@Override
	protected void addTriple(@Nonnull Resource subject, @Nonnull org.openrdf.model.URI pred, @Nonnull Value object) {
		try {
		    if (LOGGER.isDebugEnabled()) {
		        LOGGER.debug("Inserting triple into graph with name " + axiomResource);
		        LOGGER.debug("\t" + subject + ", " + pred + ", " + object);
		    }
			connection.add(subject, pred, object, axiomResource);
		}
		catch (RepositoryException e) {
			throw new RepositoryRuntimeException(e);
		}
	}

}
