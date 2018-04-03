/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.openshift.internal.restclient.model.v1;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.openshift.restclient.PredefinedResourceKind;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

import com.openshift.internal.restclient.model.Build;
import com.openshift.internal.restclient.model.KubernetesResource;
import com.openshift.internal.restclient.model.properties.ResourcePropertiesRegistry;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.utils.Samples;

/**
 * @author Jeff Cantrill
 */
public class ResourceTest {
	
	private static final String VERSION = "v1";
	private static IResource resource;
	
	@Before
	public void setup(){
		IClient client = mock(IClient.class);
		ModelNode node = ModelNode.fromJSONString(Samples.V1_SERVICE.getContentAsString());
		resource = new KubernetesResource(node, client, ResourcePropertiesRegistry.getInstance().get(VERSION, PredefinedResourceKind.SERVICE.getIdentifier())) {};
	}
	
	@Test
	public void getNamespace(){
		assertEquals("test", resource.getNamespaceName());
	}

	@Test
	public void getNamespaceWhenUndefinedShouldReturnEmptyString(){
		IClient client = mock(IClient.class);
		ModelNode node = ModelNode.fromJSONString(Samples.V1_SERVICE.getContentAsString());
		node.get("metadata").remove("namespace");
		resource = new Build(node, client, ResourcePropertiesRegistry.getInstance().get(VERSION, PredefinedResourceKind.PROJECT.getIdentifier()));
		assertEquals("", resource.getNamespaceName());
	}

	
}
