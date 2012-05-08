package org.protege.owl.rdf;

import info.aduna.iteration.CloseableIteration;

import java.util.Set;
import java.util.TreeSet;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;
import org.protege.owl.rdf.api.OwlTripleStore;
import org.protege.owl.rdf.impl.OwlTripleStoreImpl;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AnonymityTests {
	public static final String NS = "http://protege.org/ontologies/RDFAnonymityTest.owl";
	public static final OWLObjectProperty P;
	public static final OWLObjectProperty Q;
	
	public static final OWLAnnotationProperty R;
	
	public static final OWLNamedIndividual I;
	
	static {
		OWLDataFactory factory = OWLManager.getOWLDataFactory();
		P = factory.getOWLObjectProperty(IRI.create(NS + "#p"));
		Q = factory.getOWLObjectProperty(IRI.create(NS + "#q"));
		
		R = factory.getOWLAnnotationProperty(IRI.create(NS + "#r"));
		
		I = factory.getOWLNamedIndividual(IRI.create(NS + "#i"));
	}
	
	
    private OwlTripleStore ots;
    private OWLOntologyID ontologyId = new OWLOntologyID();
    
    @BeforeMethod
    public void setup() throws RepositoryException, OWLOntologyCreationException {
        Sail sailStack = new MemoryStore();
        Repository repository = new SailRepository(sailStack);
        repository.initialize();
        ots = new OwlTripleStoreImpl(repository, OWLManager.getOWLDataFactory());
    }
    
    @Test
    public void axiomWithAnonymousIndividualTest1() throws RepositoryException {
        OWLDataFactory factory = OWLManager.getOWLDataFactory();
        OWLIndividual i = factory.getOWLAnonymousIndividual();
        OWLIndividual j = factory.getOWLAnonymousIndividual();
        OWLIndividual k = factory.getOWLAnonymousIndividual();
        OWLAxiom axiom1 = factory.getOWLSameIndividualAxiom(i, j);
        OWLAxiom axiom2 = factory.getOWLSameIndividualAxiom(i, k);
        Assert.assertFalse(ots.hasAxiom(ontologyId, axiom1));
        Assert.assertFalse(ots.hasAxiom(ontologyId, axiom2));
        ots.addAxiom(ontologyId, axiom1);
        Assert.assertTrue(ots.hasAxiom(ontologyId, axiom1));
        Assert.assertFalse(ots.hasAxiom(ontologyId, axiom2));
        int count = 0;
        CloseableIteration<OWLAxiom, RepositoryException> axioms = ots.listAxioms(ontologyId);
        while (axioms.hasNext()) {
            Assert.assertEquals(axioms.next(), axiom1);
            count++;
        }
        Assert.assertEquals(count, 1);
        ots.removeAxiom(ontologyId, axiom1);
        Assert.assertFalse(ots.hasAxiom(ontologyId, axiom1));
        Assert.assertFalse(ots.hasAxiom(ontologyId, axiom2));
    }
    
    @Test
    public void axiomWithAnonymousIndividualTest2() throws RepositoryException {
        OWLDataFactory factory = OWLManager.getOWLDataFactory();
        OWLIndividual i = factory.getOWLAnonymousIndividual();
        OWLIndividual j = factory.getOWLAnonymousIndividual();
        OWLIndividual k = factory.getOWLAnonymousIndividual();
        OWLAxiom axiom1 = factory.getOWLSameIndividualAxiom(i, j);
        OWLAxiom axiom2 = factory.getOWLSameIndividualAxiom(i, k);
        ots.addAxiom(ontologyId, axiom1);
        Assert.assertTrue(ots.hasAxiom(ontologyId, axiom1));
        Assert.assertFalse(ots.hasAxiom(ontologyId, axiom2));
    }
    
    @Test
    public void axiomWithAnonymousIndividualTest3() throws RepositoryException {
        OWLDataFactory factory = OWLManager.getOWLDataFactory();
        OWLIndividual i = factory.getOWLAnonymousIndividual();
        OWLIndividual j = factory.getOWLAnonymousIndividual();
        OWLIndividual k = factory.getOWLAnonymousIndividual();
        OWLAxiom axiom1 = factory.getOWLSameIndividualAxiom(i, j);
        OWLAxiom axiom2 = factory.getOWLSameIndividualAxiom(i, k);
        ots.addAxiom(ontologyId, axiom1);
        int count = 0;
        CloseableIteration<OWLAxiom, RepositoryException> axioms = ots.listAxioms(ontologyId);
        while (axioms.hasNext()) {
            Assert.assertEquals(axioms.next(), axiom1);
            count++;
        }
        Assert.assertEquals(count, 1);
    }
    

    @Test
    public void axiomWithAnonymousAnnotationValue() throws RepositoryException {
        OWLDataFactory factory = OWLManager.getOWLDataFactory();
        OWLAnonymousIndividual i = factory.getOWLAnonymousIndividual();
        OWLAnonymousIndividual j = factory.getOWLAnonymousIndividual();
        OWLAnonymousIndividual k = factory.getOWLAnonymousIndividual();
        
        Set<OWLIndividual> individuals = new TreeSet<OWLIndividual>();
        individuals.add(i);
        individuals.add(j);
        Set<OWLAnnotation> annotations = new TreeSet<OWLAnnotation>();
        annotations.add(factory.getOWLAnnotation(factory.getRDFSComment(), k));
        OWLAxiom axiom1 = factory.getOWLSameIndividualAxiom(individuals, annotations);
        
        ots.addAxiom(ontologyId, axiom1);
        
        Assert.assertTrue(ots.hasAxiom(ontologyId, axiom1));
        int count = 0;
        CloseableIteration<OWLAxiom, RepositoryException> axioms = ots.listAxioms(ontologyId);
        while (axioms.hasNext()) {
            OWLAxiom axiomFound = axioms.next();
            Assert.assertEquals(axiomFound, axiom1);
            Assert.assertEquals(axiomFound.getAnnotations().size(), 1);
            count++;
        }
        Assert.assertEquals(count, 1);
    }
    
    @Test
    public void annotationAssertionWithAnonymousSubject() throws RepositoryException {
    	OWLDataFactory factory = OWLManager.getOWLDataFactory();
    	OWLAnonymousIndividual j = factory.getOWLAnonymousIndividual();
    	OWLAnonymousIndividual k = factory.getOWLAnonymousIndividual();
    	
    	OWLAxiom axiom = factory.getOWLAnnotationAssertionAxiom(j, factory.getOWLAnnotation(R, k));
    	
    	ots.addAxiom(ontologyId, axiom);
    	
        Assert.assertTrue(ots.hasAxiom(ontologyId, axiom));
        int count = 0;
        CloseableIteration<OWLAxiom, RepositoryException> axioms = ots.listAxioms(ontologyId);
        while (axioms.hasNext()) {
            OWLAxiom axiomFound = axioms.next();
            Assert.assertEquals(axiomFound, axiom);
            count++;
        }
        Assert.assertEquals(count, 1);
    }
}
