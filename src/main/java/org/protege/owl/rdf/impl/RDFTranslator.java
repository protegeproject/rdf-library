package org.protege.owl.rdf.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.coode.owlapi.rdf.model.AbstractTranslator;
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
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;


public class RDFTranslator extends AbstractTranslator<Value, Resource, org.openrdf.model.URI, org.openrdf.model.Literal> {
	private org.openrdf.model.URI axiomResource;
	private ValueFactory rdfFactory;
	private RepositoryConnection connection;
	
	public static void translate(Repository repository, OWLAxiom axiom, org.openrdf.model.URI hashCodeProperty) throws RepositoryException {
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
	}
	
	private static OWLOntology createOntology(OWLOntologyManager manager, OWLAxiom axiom) throws OWLOntologyCreationException {
		OWLOntology ontology = manager.createOntology();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		changes.add(new AddAxiom(ontology, axiom));
		manager.applyChanges(changes);
		return ontology;
	}

	private RDFTranslator(Repository repository, OWLOntologyManager manager, OWLOntology ontology) throws RepositoryException {
		super(manager, ontology, false);
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
			connection.add(subject, pred, object, axiomResource);
		}
		catch (RepositoryException e) {
			throw new RepositoryRuntimeException(e);
		}
	}

}
