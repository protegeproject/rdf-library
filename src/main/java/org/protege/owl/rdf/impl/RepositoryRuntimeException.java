package org.protege.owl.rdf.impl;

import org.eclipse.rdf4j.repository.RepositoryException;

public class RepositoryRuntimeException extends RuntimeException {
	private static final long serialVersionUID = -7871457225828300162L;

	public RepositoryRuntimeException(RepositoryException cause) {
		super(cause);
	}
	
	@Override
	public RepositoryException getCause() {
		return (RepositoryException) super.getCause();
	}
}
