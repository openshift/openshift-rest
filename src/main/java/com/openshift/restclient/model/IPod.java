/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.openshift.restclient.model;

import java.util.Collection;
import java.util.Set;

/**
 * @author Jeff Cantrill
 */
public interface IPod extends IResource {

	/**
	 * Gets the IP of the Pod
	 * @return
	 */
	String getIP();
	
	/**
	 * Gets the name of the host on which
	 * the pod is running
	 * @return
	 */
	String getHost();
	
	/**
	 * Gets the collection of image names
	 * for the pod containers
	 * @return
	 */
	Collection<String> getImages();
	
	/**
	 * Gets the status of the pod
	 * @return
	 */
	String getStatus();
	
	/**
	 * Retrieve the set of ports that the
	 * containers are using
	 */
	Set<IPort> getContainerPorts();
}
