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
import org.semanticweb.owlapi.util.AlwaysOutputId;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


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
	private Map<Object, BNode> bnodeMap = new IdentityHashMap<Object, BNode>();
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
		         axiom.accept(translator);
                         addRdfTypes(repository,translator,axiom);
                         addControlTriples(repository,translator,axiom,hashCodeProperty,sourceOntologyProperty,ontologyRepresentative);
			
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
        
        public static void translate(Repository repository, Set<OWLAxiom> axioms, 
	                             org.openrdf.model.URI hashCodeProperty,
	                             org.openrdf.model.URI sourceOntologyProperty,
	                             org.openrdf.model.URI ontologyRepresentative) throws RepositoryException {
	    if (LOGGER.isDebugEnabled()) {
	        LOGGER.debug("Starting axiom parse");
	    }
		boolean success = false;
		RDFTranslator translator = null;		
		try {		       
	        OWLOntology ontology = manager.createOntology(axioms);
                for (OWLAxiom axiom: axioms){  
                    translator = new RDFTranslator(repository, manager, ontology);
                    axiom.accept(translator);
                    addRdfTypes(repository,translator,axiom);
                    addControlTriples(repository,translator,axiom,hashCodeProperty,sourceOntologyProperty,ontologyRepresentative);
                    }
          	success = true;
                }
		catch (RepositoryRuntimeException rre) {
			throw rre.getCause();
		}
                catch (OWLOntologyCreationException e) {
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
        
        private static void addControlTriples(Repository repository,RDFTranslator translator, OWLAxiom axiom,org.openrdf.model.URI hashCodeProperty, org.openrdf.model.URI sourceOntologyProperty, org.openrdf.model.URI ontologyRepresentative) throws RepositoryException{
                        ValueFactory rdfFactory = repository.getValueFactory();
                        RepositoryConnection connection = translator.getConnection();
                        org.openrdf.model.Literal hashCodeValue = rdfFactory.createLiteral(axiom.hashCode());
			connection.add(translator.axiomResource, hashCodeProperty, hashCodeValue);
			connection.add(translator.axiomResource, sourceOntologyProperty, ontologyRepresentative);
        }
       
        private static void addRdfTypes(Repository repository, RDFTranslator translator, OWLAxiom axiom) throws RepositoryException{
            RepositoryConnection connection = translator.getConnection();
            ValueFactory rdfFactory = repository.getValueFactory();
            for (OWLEntity entity : axiom.getSignature()) {  // why aren't these getting included?
			       
                connection.add(rdfFactory.createURI(entity.getIRI().toString()), 
			            rdfFactory.createURI(OWLRDFVocabulary.RDF_TYPE.getIRI().toString()), 
			            rdfFactory.createURI(entity.getEntityType().getVocabulary().getIRI().toString()), 
                                translator.axiomResource);
			}
            
}
        
        private static OWLOntology createOntology(OWLOntologyManager manager, OWLAxiom axiom) throws OWLOntologyCreationException {
		Set<OWLAxiom> axiomSet = new HashSet<>();
		axiomSet.add(axiom);
		return manager.createOntology(axiomSet);
	}


	private RDFTranslator(Repository repository, OWLOntologyManager manager, OWLOntology ontology) throws RepositoryException {
		super(manager, ontology, false, new AlwaysOutputId(), new AlwaysOutputId(), new AtomicInteger(1));
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

	@Nonnull
	@Override
	protected Resource getAnonymousNodeForExpressions(@Nonnull Object o) {
		return rdfFactory.createBNode();
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
