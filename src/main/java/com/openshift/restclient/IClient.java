/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.openshift.restclient;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.openshift.restclient.authorization.IAuthorizationContext;
import com.openshift.restclient.capability.ICapable;
import com.openshift.restclient.model.IList;
import com.openshift.restclient.model.IResource;

/**
 * Client is the the simplest interface for interacting with the OpenShift
 * master server.
 * 
 * @author Jeff Cantrill
 *
 */
public interface IClient extends ICapable, Cloneable {
	
	IWatcher watch(String namespace, IOpenShiftWatchListener listener, String...kinds);
	
	/**
	 * Lists all possible resources of the given kind in the default namespace
	 * @param kind
	 * @return
	 */
	<T extends IResource> List<T> list(String kind);

	
	/**
	 * Lists the given given resource kind scoping it to a specific namespace
	 * 
	 * @param kind
	 * @param namespace    The namespace to scope the possible results of this list
	 * @return
	 */
	<T extends IResource> List<T> list(String kind, String namespace);
	
	/**
	 * Lists the given given resource kind scoping it to a specific namespace
	 * 
	 * @param kind
	 * @param namespace    The namespace to scope the possible results of this list
	 * @param labels             The label used to filter the resource
	 * @return
	 */
	<T extends IResource> List<T> list(String kind, String namespace, Map<String, String> labels);

	/**
	 * 
	 * @param service
	 * @param name
	 * @return
	 * @throws OpenShiftException if operation not supported for resource type
	 */
	<T extends IResource> T get(String kind, String name, String namespace);
	
	/**
	 * 
	 * @return  A raw list of the kind in the given namespace (e.g. ServiceList)
	 */
	IList get(String kind, String namespace);
	
	/**
	 * Creates the given resource in the namespace defined on the 
	 * resource or the default namspace if undefined
	 * @param resource
	 * @return
	 * @throws UnsupportedOperationException if the resource is a list
	 */
	<T extends IResource> T create(T resource);

	/**
	 * Creates the given resource in the given namespace
	 * @param resource
	 * @param namespace
	 * @return
	 */
	<T extends IResource> T create(T resource, String namespace);
	
	/**
	 * Creates the given resource in the given namespace using the subresource
	 * @param kind
	 * @param namespace
	 * @param name
	 * @param subresource
	 * @param payload
	 * @return
	 */
	<T extends IResource> T create(String kind, String namespace, String name, String subresource, IResource payload);
	
	/**
	 * Creates a list of resources in the given namespace
	 * @param list  The resource definitions
	 * @param namespace the namespace for the resources
	 * @return  A collection of the resources created or the status
	 *                 instance of why the creation failed.
	 *  @throws OpenShiftException if a status can not be determined from
	 *                  the exception
	 */
	Collection<IResource> create(IList list, String namespace);
	
	/**
	 * Updates the given resource
	 * @param resource
	 * @return
	 * @throws UnsupportedOperationException if the resource is a list
	 */
	<T extends IResource> T update(T resource);
	
	/**
	 * Deletes the given resource.
	 * @param resource
	 * @throws UnsupportedOperationException if the resource is a list
	 */
	<T extends IResource> void delete(T resource);
	
	
	/**
	 * Raw execution of a request
	 * @param httpMethod  HttpMethod (e.g. POST)
	 * @param kind        
	 * @param namespace
	 * @param name
	 * @param subresource  subresource or capability
	 * @param payload      the payload to sumit.  only valid on non-get operations
	 * @return
	 */
	<T extends IResource> T execute(String httpMethod, String kind, String namespace, String name, String subresource, IResource payload);

	/**
	 * Raw execution of a request
	 * @param httpMethod  HttpMethod (e.g. POST)
	 * @param kind
	 * @param namespace
	 * @param name
	 * @param subresource  subresource or capability
	 * @param payload      the payload to sumit.  only valid on non-get operations
	 * @param subcontext   additional subContext
	 * @return
	 */
	<T extends IResource> T execute(String httpMethod, String kind, String namespace, String name, String subresource, IResource payload, String subcontext);


	/**
	 * 
	 * @return the base URL of this endpoint
	 */
	URL getBaseURL();
	
	/**
	 * 
	 * @param resource
	 * @return the uri to the resource (e.g. for crafting webhooks)
	 */
	String getResourceURI(IResource resource);
	
	/**
	 * Returns the OpenShift API version for this client
	 * @return
	 * @throws UnsupportedVersionException
	 * @throws {@link UnauthorizedException}
	 */
	String getOpenShiftAPIVersion() throws UnsupportedVersionException;
	
	/**
	 * The authorization context for this client.
	 * @return  The context which will never be null
	 */
	IAuthorizationContext getAuthorizationContext();
	
	/**
	 * Returns the resource factory used to create resources based on the
	 * response from the server
	 * @return
	 */
	IResourceFactory getResourceFactory();
	
	/**
	 * Adapt this class to the given type
	 * @param klass
	 * @return an instance of the class or null if it can not
	 */
	default <T> T adapt(Class<T> klass) {
		return null;
	};
	
	/**
	 * Query the server to determine if it
	 * is ready
	 * @return
	 * @throws OpenShiftException
	 */
	String getServerReadyStatus();

	IClient clone();
}
