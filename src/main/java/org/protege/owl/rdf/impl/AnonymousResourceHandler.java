package org.protege.owl.rdf.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLObjectDuplicator;

import javax.annotation.Nonnull;

public class AnonymousResourceHandler {
    public static final String ANONYMOUS_SURROGATE_PREFIX = "urn:AnonId:";

    private OWLOntologyManager manager;
    
    private TreeMap<OWLOntologyID, IRI> ontologyIdToSurrogateIdMap = new TreeMap<>();
    private TreeMap<IRI, OWLOntologyID> surrogateIdToOntologyId = new TreeMap<>();
    
    private TreeMap<OWLAnonymousIndividual, IRI> anonymousIndividualToIRIMap = new TreeMap<>();
    private TreeMap<IRI, OWLAnonymousIndividual> iriToAnonymousIndividualMap = new TreeMap<>();
    
    public AnonymousResourceHandler(OWLOntologyManager manager) {
        this.manager = manager;
    }
    
    public IRI getSurrogateId(OWLOntologyID id) {
        IRI result = ontologyIdToSurrogateIdMap.get(id);
        if (id.isAnonymous() && result == null) {
            result = createSurrogate();
            addSurrogateOntologyId(id, result);
        }
        return result;
    }
    
    public OWLOntologyID getOntologyID(IRI iri) {
        OWLOntologyID id = surrogateIdToOntologyId.get(iri);
        if (id == null && isSurrogate(iri)) {
            id = new OWLOntologyID();
            addSurrogateOntologyId(id, iri);
        }
        return id;
    }
    
    public IRI getSurrogateId(OWLAnonymousIndividual i) {
        IRI result = anonymousIndividualToIRIMap.get(i);
        if (result == null) {
            result = createSurrogate();
            addSurrogateIndividual(i, result);
        }
        return result;
    }
    
    public OWLAnonymousIndividual getAnonymousIndividual(IRI iri) {
        OWLAnonymousIndividual i = iriToAnonymousIndividualMap.get(iri);
        if (i == null) {
            i = manager.getOWLDataFactory().getOWLAnonymousIndividual();
            addSurrogateIndividual(i, iri);
        }
        return i;
    }

    public OWLAxiom removeSurrogates(OWLAxiom axiom) {
        return new SurrogateRemover().duplicateObject(axiom);
    }
    
    public OWLAxiom insertSurrogates(OWLAxiom axiom) {
        return new SurrogateInserter().duplicateObject(axiom);
    }
    
    public boolean isSurrogate(IRI iri) {
        return iri.toString().startsWith(ANONYMOUS_SURROGATE_PREFIX);
    }
    
    private void addSurrogateOntologyId(OWLOntologyID id, IRI iri) {
        ontologyIdToSurrogateIdMap.put(id, iri);
        surrogateIdToOntologyId.put(iri, id);
    }
    
    private void addSurrogateIndividual(OWLAnonymousIndividual i, IRI iri) {
        anonymousIndividualToIRIMap.put(i, iri);
        iriToAnonymousIndividualMap.put(iri, i);
    }
    
    private IRI createSurrogate() {
        UUID id = UUID.randomUUID();
        String idString = id.toString().replaceAll("-", "_");
        return IRI.create(ANONYMOUS_SURROGATE_PREFIX + idString);
    }
    
    private Set<OWLAnnotation> duplicateAxiomAnnotations(OWLAxiom axiom, OWLObjectDuplicator duplicator) {
        Set<OWLAnnotation> duplicatedAnnos = new HashSet<>();
        for(OWLAnnotation anno : axiom.getAnnotations()) {
            duplicatedAnnos.add(duplicator.duplicateObject(anno));
        }
        return duplicatedAnnos;
    }
    
    private class SurrogateInserter extends OWLObjectDuplicator {
        public SurrogateInserter() {
            super(manager);
        }

        @Override
        public OWLAnonymousIndividual visit(OWLAnonymousIndividual i) {
            IRI iri = getSurrogateId(i);

            return manager.getOWLDataFactory().getOWLNamedIndividual(iri).asOWLAnonymousIndividual();  //TODO(AR) temp to make compile
        }

        @Override
        public OWLAnnotationAssertionAxiom visit(@Nonnull OWLAnnotationAssertionAxiom axiom) {
        	OWLObject rawSubject = duplicateObject(axiom.getSubject());
            OWLAnnotationSubject subject;
            if (rawSubject instanceof OWLNamedIndividual) {
            	subject = ((OWLNamedIndividual) rawSubject).getIRI();
            }
            else {
            	subject = (OWLAnnotationSubject) rawSubject;
            }
            OWLAnnotationProperty prop = duplicateObject(axiom.getProperty());
            OWLObject rawValue = duplicateObject(axiom.getValue());
            OWLAnnotationValue value;
            if (rawValue instanceof OWLNamedIndividual) {
                value = ((OWLNamedIndividual) rawValue).getIRI();
            }
            else {
                value = (OWLAnnotationValue) rawValue;
            }
            return manager.getOWLDataFactory().getOWLAnnotationAssertionAxiom(prop, subject, value, duplicateAxiomAnnotations(axiom, this));
        }

        @Override
        public OWLAnnotation visit(@Nonnull OWLAnnotation node) {
            OWLAnnotationProperty prop = duplicateObject(node.getProperty());
            OWLObject rawValue = duplicateObject(node.getValue());
            OWLAnnotationValue val;
            if (rawValue instanceof OWLNamedIndividual) {
                val = ((OWLNamedIndividual) rawValue).getIRI();
            }
            else {
                val = (OWLAnnotationValue) rawValue;
            }
            return manager.getOWLDataFactory().getOWLAnnotation(prop, val);
        }

    }
    
    private class SurrogateRemover extends OWLObjectDuplicator {
        
        public SurrogateRemover() {
            super(manager);
        }

        @Override
        public OWLNamedIndividual visit(@Nonnull OWLNamedIndividual i) {
            if (isSurrogate(i.getIRI())) {

                return getAnonymousIndividual(i.getIRI()).asOWLNamedIndividual();       //TODO(AR) temp to make compile
            }
            else {
                return i;
            }
        }

        @Override
        public OWLAnnotationAssertionAxiom visit(@Nonnull OWLAnnotationAssertionAxiom axiom) {
            OWLAnnotationSubject subject = duplicateObject(axiom.getSubject());
            if (subject instanceof IRI && isSurrogate((IRI) subject)) {
            	subject = getAnonymousIndividual((IRI) subject);
            }
            OWLAnnotationProperty prop = duplicateObject(axiom.getProperty());
            OWLAnnotationValue value = duplicateObject(axiom.getValue());
            if (value instanceof IRI && isSurrogate((IRI) value)) {
                value = getAnonymousIndividual((IRI) value);
            }
            return manager.getOWLDataFactory().getOWLAnnotationAssertionAxiom(prop, subject, value, duplicateAxiomAnnotations(axiom, this));
        }

        @Override
        public OWLAnnotation visit(@Nonnull OWLAnnotation node) {
            node.getProperty().accept(this);
            OWLAnnotationProperty prop = duplicateObject(node.getProperty());
            OWLAnnotationValue val = duplicateObject(node.getValue());
            if (val instanceof IRI && isSurrogate((IRI) val)) {
                val = getAnonymousIndividual((IRI) val);
            }
            return manager.getOWLDataFactory().getOWLAnnotation(prop, val);
        }
    }
}
