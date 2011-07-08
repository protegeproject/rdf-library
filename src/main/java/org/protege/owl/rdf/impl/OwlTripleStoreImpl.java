package org.protege.owl.rdf.impl;

import info.aduna.iteration.CloseableIteration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.protege.owl.rdf.api.OwlTripleStore;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
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
	
	private org.openrdf.model.URI hashCodeProperty;
	
	private Repository repository;
	
	private OWLOntology internalOntology;
	private RDFTranslator translator;


	
	private Set<IRI> anonymousNodes;
	private AnonymousNodeChecker anonymousNodeChecker = new AnonymousNodeChecker() {
        public boolean isAnonymousNode(IRI iri) {
            return anonymousNodes.contains(iri);  // what the heck?!??
        }

        public boolean isAnonymousSharedNode(String iri) {
            return false;
        }

        public boolean isAnonymousNode(String iri) {
            throw new UnsupportedOperationException("not used?");
        }
    };
	
	
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
		org.openrdf.model.URI axiomResource = getAxiomId(axiom);
		if (axiomResource != null) {
			return axiomResource;
		}
		boolean success = false;
		OWLOntologyManager internalManager = internalOntology.getOWLOntologyManager();
		axiomResource = translator.startTransaction();
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
	public void removeAxiom(OWLAxiom axiom) throws RepositoryException {
		org.openrdf.model.URI axiomResource = getAxiomId(axiom);
		RepositoryConnection connection = repository.getConnection();
		try {
			RepositoryResult<Statement> stmts = connection.getStatements(null, null, null, false, axiomResource);
			connection.remove(stmts, axiomResource);
		}
		finally {
			connection.close();
		}
	}
	
	public boolean hasAxiom(OWLAxiom axiom) throws RepositoryException {
		return getAxiomId(axiom) != null;
	}
	
	@Override
	public CloseableIteration<OWLAxiom, RepositoryException> listAxioms() {
		throw new UnsupportedOperationException("Not supported yet");
	}
	
	@Override
	public boolean integrityCheck() {
		throw new UnsupportedOperationException("Not supported yet");
	}
	
	@Override
	public boolean incorporateExternalChanges() {
		throw new UnsupportedOperationException("Not supported yet");
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
		catch (Exception ooce) {
			throw new RepositoryException(ooce);
		}
		finally {
			connection.close();
		}
	}
	
	private OWLAxiom parseAxiom(RepositoryConnection connection, org.openrdf.model.URI axiomId) throws OWLOntologyCreationException, RepositoryException, SAXException, IOException, RDFHandlerException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting parse");
        }
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.createOntology();
		org.semanticweb.owlapi.rdf.syntax.RDFConsumer consumer = new OWLRDFConsumer(ontology, anonymousNodeChecker, new OWLOntologyLoaderConfiguration());
		RepositoryResult<Statement> triples = connection.getStatements(null, null, null, false, axiomId);
		RDFWriter writer = null;
        if (LOGGER.isDebugEnabled()) {
            File tmp = File.createTempFile("owl2triples", ".owl");
            writer = new RDFXMLWriter(new FileWriter(tmp));
            LOGGER.debug("Writing to " + tmp);
            writer.startRDF();
        }
		while (triples.hasNext()) {
			Statement stmt = triples.next();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(stmt);
                writer.handleStatement(stmt);
            }
			String subjectName = generateName(stmt.getSubject());
			String predicateName = generateName(stmt.getPredicate());
			if (stmt.getObject() instanceof org.openrdf.model.Literal) {
				addTriple(consumer, subjectName, predicateName, (org.openrdf.model.Literal) stmt.getObject());
			} else {
				addTriple(consumer, subjectName, predicateName, (org.openrdf.model.Resource) stmt.getObject());
			}
		}
		consumer.endModel();
        if (LOGGER.isDebugEnabled()) {
            writer.endRDF();
            LOGGER.debug("Parse complete - " + ontology.getAxioms());
        }
		if (ontology.getAxiomCount() == 1) {
			return ontology.getAxioms().iterator().next();
		}
		else if (ontology.getAxiomCount() > 1) {
			for (OWLAxiom axiom : ontology.getAxioms()) {
				if (!(axiom instanceof OWLDeclarationAxiom)) {
					return axiom;
				}
			}
		}
		return null;
	}
	
	private void addTriple(org.semanticweb.owlapi.rdf.syntax.RDFConsumer consumer,
			               String subjectName, String predicateName, org.openrdf.model.Literal literal) throws SAXException {
		String datatype;
		if (literal.getDatatype() == null) {
			datatype = OWL2Datatype.RDF_PLAIN_LITERAL.getIRI().toString();
		}
		else {
			datatype = literal.getDatatype().stringValue();
		}
		consumer.statementWithLiteralValue(subjectName, 
				                           predicateName, 
				                           literal.stringValue(), 
				                           literal.getLanguage(), 
				                           datatype);
	}
	
	private void addTriple(org.semanticweb.owlapi.rdf.syntax.RDFConsumer consumer,
                           String subjectName, String predicateName, org.openrdf.model.Resource value) throws SAXException {
		String objectName = generateName(value);
		consumer.statementWithResourceValue(subjectName, predicateName, objectName);
	}
	
	private String generateName(org.openrdf.model.Resource resource) {
		String name;
		if (resource instanceof BNode) {
			name = "_:" + ((BNode) resource).getID();
			anonymousNodes.add(IRI.create(name));
		}
		else {
			name = ((org.openrdf.model.Resource) resource).stringValue();
		}
		return name;
	}

}
