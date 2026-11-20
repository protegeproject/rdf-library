package org.protege.owl.rdf.impl;

import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.change.AddAxiomData;
import org.semanticweb.owlapi.util.OWLObjectTransformer;

public class AnonymousResourceHandler {
    public static final String ANONYMOUS_SURROGATE_PREFIX = "urn:AnonId:";
    
    private OWLDataFactory factory;
    
    private TreeMap<OWLOntologyID, IRI> ontologyIdToSurrogateIdMap = new TreeMap<>();
    private TreeMap<IRI, OWLOntologyID> surrogateIdToOntologyId = new TreeMap<>();
    
    private TreeMap<OWLAnonymousIndividual, IRI> anonymousIndividualToIRIMap = new TreeMap<>();
    private TreeMap<IRI, OWLAnonymousIndividual> iriToAnonymousIndividualMap = new TreeMap<>();
    
    public AnonymousResourceHandler(OWLDataFactory factory) {
        this.factory = factory;
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
            i = factory.getOWLAnonymousIndividual();
            addSurrogateIndividual(i, iri);
        }
        return i;
    }

    public OWLAxiom removeSurrogates(OWLAxiom axiom) {
        if (axiom instanceof OWLAnnotationAssertionAxiom) {
            return (OWLAxiom) removeSurrogate(axiom);
        }
        return transform(transform(axiom, this::removeSurrogate, OWLIndividual.class),
                this::removeSurrogate, OWLAnnotation.class);
    }
    
    public OWLAxiom insertSurrogates(OWLAxiom axiom) {
        if (axiom instanceof OWLAnnotationAssertionAxiom) {
            return (OWLAxiom) insertSurrogate(axiom);
        }
        // Annotation values must use surrogate IRIs, while individual positions
        // use named individuals. Transform annotations before individuals.
        return transform(transform(axiom, this::insertSurrogate, OWLAnnotation.class),
                this::insertSurrogate, OWLIndividual.class);
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
    
    @SuppressWarnings("unchecked")
    private OWLAxiom transform(OWLAxiom axiom, Function<OWLObject, OWLObject> mapper, Class<?> targetType) {
        // OWLObjectTransformer also calls its function for OWLFacet values;
        // retain those unchanged instead of casting them to the target type.
        OWLObjectTransformer<Object> transformer = new OWLObjectTransformer<>(object -> true,
                object -> object instanceof OWLObject ? mapper.apply((OWLObject) object) : object,
                factory, (Class<Object>) targetType);
        return transformer.change(axiom).stream()
                .filter(AddAxiomData.class::isInstance)
                .map(change -> ((AddAxiomData) change).getAxiom())
                .findFirst().orElse(axiom);
    }

    private OWLObject insertSurrogate(OWLObject object) {
        if (object instanceof OWLAnonymousIndividual) {
            return factory.getOWLNamedIndividual(getSurrogateId((OWLAnonymousIndividual) object));
        }
        if (object instanceof OWLAnnotationAssertionAxiom) {
            OWLAnnotationAssertionAxiom axiom = (OWLAnnotationAssertionAxiom) object;
            OWLAnnotationSubject subject = axiom.getSubject();
            if (subject instanceof OWLAnonymousIndividual) {
                subject = getSurrogateId((OWLAnonymousIndividual) subject);
            }
            return factory.getOWLAnnotationAssertionAxiom(axiom.getProperty(), subject,
                    insertAnnotationValue(axiom.getValue()), axiom.annotations()
                            .map(this::insertAnnotation).collect(Collectors.toSet()));
        }
        if (object instanceof OWLAnnotation) {
            return insertAnnotation((OWLAnnotation) object);
        }
        return object;
    }

    private OWLAnnotationValue insertAnnotationValue(OWLAnnotationValue value) {
        return value instanceof OWLAnonymousIndividual
                ? getSurrogateId((OWLAnonymousIndividual) value) : value;
    }

    private OWLAnnotation insertAnnotation(OWLAnnotation annotation) {
        return factory.getOWLAnnotation(annotation.getProperty(),
                insertAnnotationValue(annotation.getValue()), annotation.annotations()
                        .map(this::insertAnnotation).collect(Collectors.toSet()));
    }

    private OWLObject removeSurrogate(OWLObject object) {
        if (object instanceof OWLNamedIndividual) {
            OWLNamedIndividual individual = (OWLNamedIndividual) object;
            return isSurrogate(individual.getIRI()) ? getAnonymousIndividual(individual.getIRI()) : object;
        }
        if (object instanceof OWLAnnotationAssertionAxiom) {
            OWLAnnotationAssertionAxiom axiom = (OWLAnnotationAssertionAxiom) object;
            OWLAnnotationSubject subject = axiom.getSubject();
            if (subject instanceof IRI && isSurrogate((IRI) subject)) {
                subject = getAnonymousIndividual((IRI) subject);
            }
            return factory.getOWLAnnotationAssertionAxiom(axiom.getProperty(), subject,
                    removeAnnotationValue(axiom.getValue()), axiom.annotations()
                            .map(this::removeAnnotation).collect(Collectors.toSet()));
        }
        if (object instanceof OWLAnnotation) {
            return removeAnnotation((OWLAnnotation) object);
        }
        return object;
    }

    private OWLAnnotationValue removeAnnotationValue(OWLAnnotationValue value) {
        return value instanceof IRI && isSurrogate((IRI) value)
                ? getAnonymousIndividual((IRI) value) : value;
    }

    private OWLAnnotation removeAnnotation(OWLAnnotation annotation) {
        return factory.getOWLAnnotation(annotation.getProperty(),
                removeAnnotationValue(annotation.getValue()), annotation.annotations()
                        .map(this::removeAnnotation).collect(Collectors.toSet()));
    }
}
