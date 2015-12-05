package org.protege.owl.rdf.impl;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.rdf.model.AbstractTranslator;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AlwaysOutputId;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;


public class RDFTranslator extends AbstractTranslator<Value, Resource, org.openrdf.model.URI, org.openrdf.model.Literal> {
    public static final Logger LOGGER = Logger.getLogger(RDFTranslator.class);
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
	private Map<Object, BNode> bnodeMap = new IdentityHashMap<Object, BNode>();
	
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
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
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
		OWLOntology ontology = manager.createOntology();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		changes.add(new AddAxiom(ontology, axiom));
		manager.applyChanges(changes);
		return ontology;
	}

	private RDFTranslator(Repository repository, OWLOntologyManager manager, OWLOntology ontology) throws RepositoryException {
		super(manager, ontology, false, new AlwaysOutputId());
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
	
	public org.openrdf.model.URI getAxiomResource() {
		return axiomResource;
	}

	@Override
	protected org.openrdf.model.URI getResourceNode(IRI iri) {
		return rdfFactory.createURI(iri.toString());
	}

	@Override
	protected org.openrdf.model.URI getPredicateNode(IRI iri) {
		return rdfFactory.createURI(iri.toString());
	}

	@Override
	protected BNode getAnonymousNode(Object key) {
		BNode node = bnodeMap.get(key);
		if (node == null) {
			node = rdfFactory.createBNode();
			bnodeMap.put(key, node);
		}
		return node;
	}

	@Override
	protected org.openrdf.model.Literal getLiteralNode(OWLLiteral literal) {
		if (literal.isRDFPlainLiteral() && literal.getLang() != null) {
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
	protected void addTriple(Resource subject, org.openrdf.model.URI pred, Value object) {
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
