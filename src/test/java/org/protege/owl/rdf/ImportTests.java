package org.protege.owl.rdf;

import info.aduna.iteration.CloseableIteration;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;
import org.protege.owl.rdf.api.OwlTripleStore;
import org.protege.owl.rdf.impl.OwlTripleStoreImpl;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ImportTests {
	public static final OWLOntologyID ONTOLOGY1 = new OWLOntologyID(IRI.create("http://protege.org/ontology1"), IRI.create("http://protege.org/version1"));
	public static final OWLOntologyID ONTOLOGY2 = new OWLOntologyID(IRI.create("http://protege.org/ontology1"), IRI.create("http://protege.org/version2"));
	public static final OWLOntologyID ONTOLOGY3 = new OWLOntologyID(IRI.create("http://protege.org/ontology2"));
	public static final OWLOntologyID ONTOLOGY4 = new OWLOntologyID(IRI.create("http://protege.org/ontology3"));

    public static final String NS = "http://protege.stanford.edu/ontologies/DomainInference.owl";
    public static final OWLClass C = OWLManager.getOWLDataFactory().getOWLClass(IRI.create(NS + "#C"));
    public static final OWLClass D = OWLManager.getOWLDataFactory().getOWLClass(IRI.create(NS + "#D"));
    public static final OWLClass E = OWLManager.getOWLDataFactory().getOWLClass(IRI.create(NS + "#E"));
	
    private OwlTripleStore ots;
    
    @BeforeMethod
    public void setup() throws RepositoryException, OWLOntologyCreationException {
        Sail sailStack = new MemoryStore();
        Repository repository = new SailRepository(sailStack);
        repository.initialize();
        ots = new OwlTripleStoreImpl(repository, OWLManager.getOWLDataFactory());
    }
    
	@Test
	public void testOntologyRepresentative() throws RepositoryException, OWLOntologyCreationException {
		OWLOntologyID id1 = new OWLOntologyID();
		OWLOntologyID id2 = new OWLOntologyID();
		basicTest(ONTOLOGY1, ONTOLOGY2);
		basicTest(ONTOLOGY1, ONTOLOGY3);
		basicTest(ONTOLOGY3, ONTOLOGY4);
		basicTest(ONTOLOGY1, id1);
		basicTest(ONTOLOGY3, id1);
		basicTest(id1, id2);
	}
	
	public void basicTest(OWLOntologyID id1, OWLOntologyID id2) throws RepositoryException, OWLOntologyCreationException {
	    setup();
		OWLDataFactory factory = OWLManager.getOWLDataFactory();
		OWLAxiom axiom1 = factory.getOWLEquivalentClassesAxiom(C, D);
		OWLAxiom axiom2 = factory.getOWLDisjointClassesAxiom(D, E);
		ots.addAxiom(id1, axiom1);
		ots.addAxiom(id2, axiom2);
		Assert.assertTrue(ots.hasAxiom(id1, axiom1));
		Assert.assertFalse(ots.hasAxiom(id2, axiom1));
		Assert.assertTrue(ots.hasAxiom(id2, axiom2));
		Assert.assertFalse(ots.hasAxiom(id1, axiom2));
	}
	
	@Test
	public void testListAxioms() throws RepositoryException {
	    OWLDataFactory factory = OWLManager.getOWLDataFactory();
	    OWLAxiom axiom1 = factory.getOWLEquivalentClassesAxiom(C, D);
	    OWLAxiom axiom2 = factory.getOWLDisjointClassesAxiom(D, E);
	    ots.addAxiom(ONTOLOGY1, axiom1);
	    ots.addAxiom(ONTOLOGY2, axiom2);
	    int counter = 0;
	    CloseableIteration<OWLAxiom, RepositoryException> it = ots.listAxioms(ONTOLOGY1);
	    while (it.hasNext()) {
	        OWLAxiom axiom = it.next();
	        Assert.assertEquals(axiom, axiom1);
	        counter++;
	    }
	    Assert.assertEquals(counter, 1);
	    counter = 0;
	    it = ots.listAxioms(ONTOLOGY2);
        while (it.hasNext()) {
            OWLAxiom axiom = it.next();
            Assert.assertEquals(axiom, axiom2);
            counter++;
        }
        Assert.assertEquals(counter, 1);
	}
	
	@Test
	public void testDelete() throws RepositoryException {
        OWLDataFactory factory = OWLManager.getOWLDataFactory();
        OWLAxiom axiom1 = factory.getOWLEquivalentClassesAxiom(C, D);
        OWLAxiom axiom2 = factory.getOWLDisjointClassesAxiom(D, E);
        ots.addAxiom(ONTOLOGY1, axiom1);
        ots.addAxiom(ONTOLOGY2, axiom2);
        ots.removeAxiom(ONTOLOGY2, axiom1);
        Assert.assertTrue(ots.hasAxiom(ONTOLOGY1, axiom1));
        Assert.assertFalse(ots.hasAxiom(ONTOLOGY2, axiom1));
        ots.removeAxiom(ONTOLOGY1, axiom1);
        Assert.assertFalse(ots.hasAxiom(ONTOLOGY1, axiom1));
        Assert.assertFalse(ots.hasAxiom(ONTOLOGY2, axiom1));
        Assert.assertTrue(ots.hasAxiom(ONTOLOGY2,axiom2));
	}

}
