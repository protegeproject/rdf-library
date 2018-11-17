package org.protege.owl.rdf.impl;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.rdf.model.AbstractTranslator;
import org.semanticweb.owlapi.util.OWLAnonymousIndividualsWithMultipleOccurrences;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFTranslator extends AbstractTranslator<Value, Resource, IRI, Literal> {

	public static final Logger LOGGER = LoggerFactory.getLogger(RDFTranslator.class);

	private IRI axiomResource;

	/**
	 * There is a dangerous bend coming up! If you don't use the identity hash map then this code doesn't work.
	 * The identity hashmap is used to ensure that lists are not reused in non-standard ways by different
	 * axioms. (I think that this might actually lead to an OWL full by the specification because of how triples
	 * are "consumed" by the translator). But there is an additional problem (not quite a bug because it works)
	 * in the AbstractTranslator.translateList method in an off by one error when it generates the key for
	 * getAnonymousNode. So using a HashMap here leads to unexpectedly bad results even when you would not
	 * expect that lists would be shared.
	 */
	private Map<Object, BNode> bnodeMap = new IdentityHashMap<Object, BNode>();

	private static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

	private ValueFactory rdfFactory;

	private RepositoryConnection connection;

	public static void translate(Repository repository, OWLAxiom axiom, IRI hashCodeProperty,
			IRI sourceOntologyProperty, IRI ontologyRepresentative)
		throws RepositoryException
	{
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
			for (OWLEntity entity : axiom.getSignature()) { // why aren't these getting included?
				connection.add(rdfFactory.createIRI(entity.getIRI().toString()),
						rdfFactory.createIRI(OWLRDFVocabulary.RDF_TYPE.getIRI().toString()),
						rdfFactory.createIRI(entity.getEntityType().getVocabulary().getIRI().toString()),
						translator.axiomResource);
			}

			Literal hashCodeValue = rdfFactory.createLiteral(axiom.hashCode());
			connection.add(translator.axiomResource, hashCodeProperty, hashCodeValue);
			connection.add(translator.axiomResource, sourceOntologyProperty, ontologyRepresentative);
			success = true;
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

	private static OWLOntology createOntology(OWLOntologyManager manager, OWLAxiom axiom)
		throws OWLOntologyCreationException
	{
		Set<OWLAxiom> axiomSet = new HashSet<>();
		axiomSet.add(axiom);
		return manager.createOntology(axiomSet);
	}

	private RDFTranslator(Repository repository, OWLOntologyManager manager, OWLOntology ontology)
		throws RepositoryException
	{
		super(manager, ontology, false, new OWLAnonymousIndividualsWithMultipleOccurrences());
		rdfFactory = repository.getValueFactory();
		axiomResource = rdfFactory.createIRI(
				OwlTripleStoreImpl.NS + "/" + UUID.randomUUID().toString().replace('-', '_'));
		connection = repository.getConnection();
	}

	public void close(boolean success)
		throws RepositoryException
	{
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

	public IRI getAxiomResource() {
		return axiomResource;
	}

	@Override
	protected IRI getResourceNode(org.semanticweb.owlapi.model.IRI iri) {
		return rdfFactory.createIRI(iri.toString());
	}

	@Override
	protected IRI getPredicateNode(org.semanticweb.owlapi.model.IRI iri) {
		return rdfFactory.createIRI(iri.toString());
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

	//	@Nonnull
	//	@Override
	//	protected Resource getAnonymousNodeForExpressions(@Nonnull Object o) {
	//		return rdfFactory.createBNode();
	//	}

	@Override
	protected Literal getLiteralNode(OWLLiteral literal) {
		if (literal.isRDFPlainLiteral() && literal.getLang() != null) {
			return rdfFactory.createLiteral(literal.getLiteral(), literal.getLang());
		}
		else if (literal.isRDFPlainLiteral()) {
			return rdfFactory.createLiteral(literal.getLiteral());
		}
		else {
			return rdfFactory.createLiteral(literal.getLiteral(),
					rdfFactory.createIRI(literal.getDatatype().getIRI().toString()));
		}
	}

	@Override
	protected void addTriple(Resource subject, IRI pred, Value object) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Inserting triple into graph with name " + axiomResource);
			LOGGER.debug("\t" + subject + ", " + pred + ", " + object);
		}
		connection.add(subject, pred, object, axiomResource);
	}
}
