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
package com.openshift.internal.restclient.api.capabilities;

import com.openshift.internal.restclient.IntegrationTestHelper;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.api.capabilities.IPodExec;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.IStoppable;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IResource;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;


public class PodExecIntegrationTest {

	private IntegrationTestHelper helper = new IntegrationTestHelper();
	private IPod pod;

	public static class TestExecListener implements IPodExec.IPodExecOutputListener {

		private static final Logger LOG = LoggerFactory.getLogger(PodExecIntegrationTest.class);


		public final CountDownLatch testDone = new CountDownLatch(1);
		public final AtomicBoolean openCalled = new AtomicBoolean(false);
		public final AtomicBoolean closeCalled = new AtomicBoolean(false);
		public final AtomicBoolean failureCalled = new AtomicBoolean(false);
		public final AtomicBoolean execErrCalled = new AtomicBoolean(false);
		public final List<String> messages = Collections.synchronizedList( new ArrayList<String>() );

		@Override
		public void onOpen() {
			assertTrue(openCalled.compareAndSet(false,true));
		}

		@Override
		public void onStdOut(String message) {
			LOG.debug( "onStdOut: " + message );
			message = message.trim();
			if ( message.isEmpty() == false ) { // Observing that actual output appears after empty newline
				messages.add( PodExec.CHANNEL_STDOUT+message );
			}
		}

		@Override
		public void onStdErr(String message) {
			LOG.debug( "onStdErr: " + message );
			message = message.trim();
			messages.add( PodExec.CHANNEL_STDERR+message );
		}

		@Override
		public void onExecErr(String message) {
			LOG.debug( "onExecError: " + message );
			execErrCalled.set(true);
			message = message.trim();
			messages.add( PodExec.CHANNEL_EXECERR+message );
		}

		@Override
		public void onClose(int code, String reason) {
			assertTrue(closeCalled.compareAndSet(false,true));
			testDone.countDown();
		}

		@Override
		public void onFailure(IOException e) {
			failureCalled.set(true);
			LOG.error( "Potentially expected error occurred", e );
			testDone.countDown();
		}

	}

	@Before
	public void setUp() throws Exception {
		IClient client = helper.createClientForBasicAuth();
		List<IResource> pods = client.list(ResourceKind.POD, "default");
		pod = (IPod) pods.stream().filter(p->p.getName().startsWith("docker-registry")).findFirst().orElse(null);
		assertNotNull("Need a pod to continue the test. Expected to find the registry", pod);
	}

	@Test
	public void testPodExec() throws Exception {
		String[] echoCommand = { "echo", "a", "b", "c" };
		TestExecListener echoListener = new TestExecListener();

		pod.accept(new CapabilityVisitor<IPodExec, IStoppable>() {
			@Override
			public IStoppable visit(IPodExec capability) {
				return capability.start( echoListener, null, echoCommand );
			}
		}, null);

		echoListener.testDone.await( 60, TimeUnit.SECONDS );
		assertTrue( echoListener.openCalled.get() );
		assertTrue( echoListener.closeCalled.get() );
		assertTrue( !echoListener.failureCalled.get() );
		assertTrue( !echoListener.execErrCalled.get() );
		assertEquals( 1, echoListener.messages.size() );
		assertEquals( "1a b c", echoListener.messages.get(0));
	}

	@Test
	public void testPodExecWithContainerSpecified() throws Exception {
		String[] echoCommand = { "echo", "a", "b", "c" };
		TestExecListener echoListener = new TestExecListener();

		final String container = pod.getContainers().iterator().next().getName();
		IPodExec.Options options = new IPodExec.Options();
		options.container( container );

		pod.accept(new CapabilityVisitor<IPodExec, IStoppable>() {
			@Override
			public IStoppable visit(IPodExec capability) {
				return capability.start( echoListener, options, echoCommand );
			}
		}, null);

		echoListener.testDone.await( 60, TimeUnit.SECONDS );
		assertTrue( echoListener.openCalled.get() );
		assertTrue( echoListener.closeCalled.get() );
		assertTrue( !echoListener.failureCalled.get() );
		assertTrue( !echoListener.execErrCalled.get() );
		assertEquals( 1, echoListener.messages.size() );
		assertEquals( "1a b c", echoListener.messages.get(0));
	}

	@Test
	public void testCommandNotFound() throws Exception {
		String[] badCommand = { "/bin/doesnotexist" };
		TestExecListener badListener = new TestExecListener();

		pod.accept(new CapabilityVisitor<IPodExec, IStoppable>() {
			@Override
			public IStoppable visit(IPodExec capability) {
				return capability.start( badListener, null, badCommand );
			}
		}, null);

		badListener.testDone.await( 60, TimeUnit.SECONDS );
		assertTrue( badListener.openCalled.get() );
		assertTrue( badListener.closeCalled.get() );
		assertTrue( badListener.execErrCalled.get() );
		// both execErr and stdErr will be called
		assertEquals( 2, badListener.messages.size() );
	}

	@Test
	public void testPodExecStop() throws Exception {
		String[] longCommand = { "sleep", "500" };
		TestExecListener longListener = new TestExecListener();

		IStoppable stopLong = pod.accept(new CapabilityVisitor<IPodExec, IStoppable>() {
			@Override
			public IStoppable visit(IPodExec capability) {
				return capability.start( longListener, null, longCommand );
			}
		}, null);

		// Trigger a cancel on the web socket before long delay can complete
		stopLong.stop();
		longListener.testDone.await( 60, TimeUnit.SECONDS );
		assertTrue( longListener.failureCalled.get() );
	}

	@Test
	public void testContainerDoesNotExist() throws Exception {
		String[] dateCommand = { "date" };
		TestExecListener dateListener = new TestExecListener();
		IPodExec.Options options = new IPodExec.Options();
		options.container( "will-not-exist-in-docker-registry-pod" );

		pod.accept(new CapabilityVisitor<IPodExec, IStoppable>() {
			@Override
			public IStoppable visit(IPodExec capability) {
				return capability.start( dateListener, options, dateCommand );
			}
		}, null);

		dateListener.testDone.await( 60, TimeUnit.SECONDS );
		assertTrue( dateListener.failureCalled.get() );
	}


}
