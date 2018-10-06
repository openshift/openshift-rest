/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 *
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *     Roland T. Lichti - implementation of user.openshift.io/v1/identities
 ******************************************************************************/

package com.openshift.internal.restclient.model.user;

import java.util.Map;

import com.openshift.internal.restclient.model.KubernetesResource;
import com.openshift.internal.restclient.model.ObjectReference;
import com.openshift.restclient.IClient;
import com.openshift.restclient.model.IObjectReference;
import com.openshift.restclient.model.user.IIdentity;
import org.jboss.dmr.ModelNode;

public class OpenShiftIdentity extends KubernetesResource implements IIdentity {

    private static final String PROVIDER_NAME = "providerName";
    private static final String PROVIDER_USER_NAME = "providerUserName";
    private static final String EXTRA = "extra";
    private static final String USER = "user";

    public OpenShiftIdentity(ModelNode node, IClient client, Map<String, String[]> propertyKeys) {
        super(node, client, propertyKeys);
    }

    @Override
    public String getUserName() {
        return asString(PROVIDER_USER_NAME);
    }

    @Override
    public String getUID() {
        return asString("metadata.uid");
    }

    @Override
    public String getProviderName() {
        return asString(PROVIDER_NAME);
    }

    @Override
    public Map<String, String> getExtra() {
        return asMap(EXTRA);
    }

    @Override
    public IObjectReference getUser() {
        return new ObjectReference(get(USER));
    }
}
