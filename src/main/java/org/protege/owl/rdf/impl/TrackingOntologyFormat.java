package org.protege.owl.rdf.impl;

import org.semanticweb.owlapi.io.RDFResourceParseError;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;

public class TrackingOntologyFormat extends RDFXMLOntologyFormat {
	private boolean failed = false;
	
	@Override
	public void addError(RDFResourceParseError error) {
		super.addError(error);
		failed = true;
	}
	
	public boolean getFailed() {
		return failed;
	}
}
