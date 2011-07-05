package org.protege.owl.rdf;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;


public class Test {

	/**
	 * @param args
	 * @throws RepositoryException 
	 */
	public static void main(String[] args) throws RepositoryException {
		Sail sailStack = new MemoryStore();
		Repository repository = new SailRepository(sailStack);
		repository.initialize();
		System.out.println(repository.toString());
	}

}
