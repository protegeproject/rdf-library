package org.protege.owl.rdf.impl;

import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.RDFResourceParseError;

public class TrackingOntologyFormat extends RDFXMLDocumentFormat {
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
