/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.openshift.restclient.capability.resources;

import com.openshift.restclient.capability.ICapability;
import com.openshift.restclient.model.IBuild;

/**
 * Capability to trigger a build based on the build configuration
 * @author Jeff Cantrill
 *
 */
public interface IBuildTriggerable extends ICapability {
	
	/**
	 * Trigger a build based on a build config
	 * @return The build that was triggered
	 */
	IBuild trigger();
	
	/**
	 * Trigger a build with the given source level commit id
	 * @param commitId
	 * @return The build that was triggered
	 */
	IBuild trigger(String commitId);

}
