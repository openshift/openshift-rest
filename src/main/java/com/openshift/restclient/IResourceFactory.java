/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.openshift.restclient;

import java.io.InputStream;
import java.util.List;

import com.openshift.restclient.model.IResource;

/**
 * Factory class for creating resources from a 
 * response string
 * 
 * @author Jeff Cantrill
 */
public interface IResourceFactory {
	
	/**
	 * Create a list of resources of the given kind
	 * from a response string
	 * @param json
	 * @param kind
	 * @return
	 * @throws ResourceFactoryException  if it is unable to create resources
	 */
	List<IResource> createList(String json, String kind);
	
	/**
	 * Create a resource from a response string
	 * @param response
	 * @return
	 * @throws ResourceFactoryException  if it is unable to create resources
	 */
	<T extends IResource> T create(String response) ;

	@Deprecated
	IResource create(String response, boolean strict) ;

	/**
	 * Create a resource from a response string
	 * @param input Read the given input stream which assumes the input
	 *                         is parsable JSON representing a valid resource
	 * @return
	 * @throws ResourceFactoryException  if it is unable to create resources
	 */
	<T extends IResource> T create(InputStream input) ;

	@Deprecated
	IResource create(InputStream input, boolean strict) ;

	/**
	 * Create(or stub) a resource for a given version and kind
	 * @param version
	 * @param kind
	 * @return
	 */
	<T extends IResource> T create(String version, String kind);

	@Deprecated
	IResource create(String version, String kind, boolean strict);

	/**
	 * Stub out the given resource kind using a version determined by the factory
	 * @param kind
	 * @param name
	 * @return
	 */
	<T extends IResource> T stub(String kind, String name);

	/**
	 * Stub out the given resource kind using a version determined by the factory
	 * @param kind
	 * @param name
	 * @param namespace
	 * @return
	 */
	<T extends IResource> T stub(String kind, String name, String namespace);
	
	/**
	 * The client given to resources when they are created 
	 * @param client
	 */
	void setClient(IClient client);
	
}
