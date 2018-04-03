/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.openshift.internal.restclient.model.v1;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.openshift.restclient.PredefinedResourceKind;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

import com.openshift.internal.restclient.model.properties.ResourcePropertiesRegistry;
import com.openshift.internal.restclient.model.user.OpenShiftUser;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.user.IUser;
import com.openshift.restclient.utils.Samples;

/**
 * Test to validate the lookup paths are correct for the version
 * @author Jeff Cantrill
 */
public class UserTest{

	private static final String VERSION = "v1";
	private IUser user;
	
	@Before
	public void setUp(){
		IClient client = mock(IClient.class);
		ModelNode node = ModelNode.fromJSONString(Samples.V1_USER.getContentAsString());
		user = new OpenShiftUser(node, client, ResourcePropertiesRegistry.getInstance().get(VERSION, PredefinedResourceKind.USER.getIdentifier()));
	}
	
	@Test
	public void testFullName() {
		assertEquals("Foo Master", user.getFullName());
	}

	@Test
	public void testUid() {
		assertEquals("94b42e96-0faa-11e5-9467-080027893417", user.getUID());
	}

}
