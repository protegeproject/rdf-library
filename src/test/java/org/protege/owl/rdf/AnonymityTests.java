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
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AnonymityTests {
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
    
    
    
}
