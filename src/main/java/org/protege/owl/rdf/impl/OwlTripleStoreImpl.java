package org.protege.owl.rdf.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.coode.owlapi.rdfxml.parser.AnonymousNodeChecker;
import org.coode.owlapi.rdfxml.parser.OWLRDFConsumer;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.protege.owl.rdf.api.OwlTripleStore;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.xml.sax.SAXException;

public class OwlTripleStoreImpl implements OwlTripleStore {
	public static final Logger LOGGER = Logger.getLogger(OwlTripleStoreImpl.class);
	
	public static final String NS = "http://protege.org/owl2triplestore.owl";
	public static final String HASH_CODE = NS + "#hashCode";

	private Repository repository;
	
	private OWLOntology internalOntology;
	private RDFTranslator translator;

	private org.openrdf.model.URI hashCodeProperty;
	private Set<IRI> anonymousNodes;
	
	
	public OwlTripleStoreImpl(Repository repository) {
		this.repository = repository;
		hashCodeProperty = repository.getValueFactory().createURI(HASH_CODE);
		anonymousNodes = new HashSet<IRI>();
		OWLOntologyManager internalManager = OWLManager.createOWLOntologyManager();
		try {
		    internalOntology = internalManager.createOntology();
			translator = new RDFTranslator(repository, internalManager, internalOntology);
		}
		catch (OWLOntologyCreationException oce) {
			throw new RuntimeException(oce); // come on guys this will never happen!
		}
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	@Override
	public Resource addAxiom(OWLAxiom axiom) throws RepositoryException {
		boolean success = false;
		OWLOntologyManager internalManager = internalOntology.getOWLOntologyManager();
		org.openrdf.model.URI axiomResource = translator.startTransaction();
		try {
			internalManager.addAxiom(internalOntology, axiom);
			
			RepositoryConnection connection = translator.getConnection();
			ValueFactory factory = repository.getValueFactory();
			axiom.accept(translator);
			for (OWLEntity entity : axiom.getSignature()) {
				connection.add(factory.createURI(entity.getIRI().toString()), 
						       factory.createURI(OWLRDFVocabulary.RDF_TYPE.getIRI().toString()), 
						       factory.createURI(entity.getEntityType().getVocabulary().getIRI().toString()), 
						       axiomResource);
			}
			org.openrdf.model.Literal hashCodeValue = factory.createLiteral(axiom.hashCode());
			connection.add(axiomResource, hashCodeProperty, hashCodeValue);
			success = true;
		}
		catch (RepositoryRuntimeException rre) {
			throw rre.getCause();
		}
		finally {
			translator.finishTransaction(success);
			internalManager.removeAxiom(internalOntology, axiom);
		}
		return axiomResource;
	}

	@Override
	public void removeAxiom(OWLAxiom axiom) {
		throw new UnsupportedOperationException("Not implemented yet");
	}
	
	public boolean hasAxiom(OWLAxiom axiom) throws RepositoryException {
		return getAxiomId(axiom) != null;
	}

	private org.openrdf.model.URI getAxiomId(OWLAxiom axiom) throws RepositoryException {
		ValueFactory factory = repository.getValueFactory();
		RepositoryConnection connection = repository.getConnection();
		try {
			org.openrdf.model.Literal hashCodeValue = factory.createLiteral(axiom.hashCode());
			RepositoryResult<Statement> correctHashCodes = connection.getStatements(null, hashCodeProperty, hashCodeValue, false);
			while (correctHashCodes.hasNext()) {
				Statement stmt = correctHashCodes.next();
				if (stmt.getSubject() instanceof org.openrdf.model.URI) {
					org.openrdf.model.URI axiomId = (org.openrdf.model.URI) stmt.getSubject();
					if (axiom.equals(parseAxiom(connection, axiomId))) {
						return axiomId;
					}
				}
			}
			return null;
		}
		catch (OWLOntologyCreationException ooce) {
			throw new RepositoryException(ooce);
		}
		catch (SAXException se) {
			throw new RepositoryException(se);
		}
		finally {
			connection.close();
		}
	}
	
	private OWLAxiom parseAxiom(RepositoryConnection connection, org.openrdf.model.URI axiomId) throws OWLOntologyCreationException, RepositoryException, SAXException {
		LOGGER.info("Starting parse");
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.createOntology();
		org.semanticweb.owlapi.rdf.syntax.RDFConsumer consumer = new OWLRDFConsumer(ontology, new AnonymousNodeChecker() {
            public boolean isAnonymousNode(IRI iri) {
                return anonymousNodes.contains(iri);  // what the heck?!??
            }

            public boolean isAnonymousSharedNode(String iri) {
                return anonymousNodes.contains(IRI.create(iri));  // what the heck?!??
            }

            public boolean isAnonymousNode(String iri) {
                return anonymousNodes.contains(IRI.create(iri));
            }
        }, new OWLOntologyLoaderConfiguration());
		RepositoryResult<Statement> triples = connection.getStatements(null, null, null, false, axiomId);
		while (triples.hasNext()) {
			Statement stmt = triples.next();
			LOGGER.info(stmt);
			String subjectName = null;
			String objectName = null;
			if (stmt.getSubject() instanceof BNode) {
				subjectName = "_:" + ((BNode) stmt.getSubject()).getID();
				anonymousNodes.add(IRI.create(subjectName));
			}
			else {
				subjectName = ((org.openrdf.model.Resource) stmt.getSubject()).stringValue();
			}
			if (stmt.getObject() instanceof BNode) {
				objectName = "_:" + ((BNode) stmt.getObject()).getID();
				anonymousNodes.add(IRI.create(objectName));
			}
			else if (stmt.getObject() instanceof org.openrdf.model.URI){
				objectName = ((org.openrdf.model.URI) stmt.getObject()).stringValue();
			}
			if (stmt.getObject() instanceof org.openrdf.model.Literal) {
				org.openrdf.model.Literal literal = (org.openrdf.model.Literal) stmt.getObject();
				String datatype;
				if (literal.getDatatype() == null) {
					datatype = OWL2Datatype.RDF_PLAIN_LITERAL.getIRI().toString();
				}
				else {
					datatype = literal.getDatatype().stringValue();
				}
				consumer.statementWithLiteralValue(subjectName, 
						                           stmt.getPredicate().stringValue(), 
						                           literal.stringValue(), 
						                           literal.getLanguage(), 
						                           datatype);
			} else {
				consumer.statementWithResourceValue(subjectName, stmt.getPredicate().stringValue(), objectName);
			}
		}
		LOGGER.info("Parse complete - " + ontology.getAxioms());
		if (ontology.getAxiomCount() == 0) {
			return null;
		}
		return ontology.getAxioms().iterator().next();
	}

}
