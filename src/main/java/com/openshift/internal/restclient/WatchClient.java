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
package com.openshift.internal.restclient;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.jboss.dmr.ModelNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.internal.restclient.model.KubernetesResource;
import com.openshift.restclient.IClient;
import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IOpenShiftWatchListener.ChangeType;
import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.authorization.ResourceForbiddenException;
import com.openshift.restclient.http.IHttpConstants;
import com.openshift.restclient.model.IList;
import com.openshift.restclient.model.IResource;

/**
  * Encapsulation of the logic to handle watching resources.
  * @author Jeff Cantrill
  *
  */
public class WatchClient implements IHttpConstants, IWatcher{
	private static final Logger LOGGER = LoggerFactory.getLogger(WatchClient.class);
	private URL baseUrl;
	private Map<String, String> typeMappings;
	private IResourceFactory factory;
	private IClient client;
	private static WebSocketClient wsClient;
	private static AtomicReference<Status> status = new AtomicReference<>(Status.Stopped);
	private static Lock lock = new ReentrantLock();
	private static Condition isStarted = lock.newCondition();
	private static long lockTimeout; 
	
	private enum Status{
		Started,
		Starting,
		Stopped,
		Stopping
	}
	
	static {
		try {
			lockTimeout = Long.parseLong(System.getProperty("com.openshift.restclient.watchlocktimeoutms", "30000")); 
		}catch(NumberFormatException e) {
			lockTimeout = 30 * 1000;
		}
		try {
			wsClient = newWebSocketClient();
			wsClient.addLifeCycleListener(new LifeCycle.Listener() {
				
				@Override
				public void lifeCycleStopping(LifeCycle event) {
					status.set(Status.Stopping);
				}
				
				@Override
				public void lifeCycleStopped(LifeCycle event) {
					status.set(Status.Stopped);
				}
				
				@Override
				public void lifeCycleStarting(LifeCycle event) {
					status.set(Status.Starting);
				}
				
				@Override
				public void lifeCycleStarted(LifeCycle event) {
					status.set(Status.Started);
					try{
						if(lock.tryLock(lockTimeout, TimeUnit.MILLISECONDS)){
							isStarted.signalAll();
						}
					} catch (InterruptedException e) {
						LOGGER.debug("Exception while trying to get lock",e);
					}finally {
						lock.unlock();
					}
				}
				
				@Override
				public void lifeCycleFailure(LifeCycle event, Throwable cause) {
					LOGGER.error("The watchclient failed:", cause);
					status.set(Status.Stopped);
				}
			});
			
		}catch(Exception e) {
			LOGGER.error("Exception initializing watch client",e);
			throw e;
		}
	}
	
	public WatchClient(URL baseUrl, Map<String, String> typeMappings, IClient client) {
		this.baseUrl = baseUrl;
		this.typeMappings = typeMappings;
		this.factory = client.getResourceFactory();
		this.client = client;
	}
	
	private class WatchEndpoint extends WebSocketAdapter{
		private IOpenShiftWatchListener listener;
		private List<IResource> resources;
		private final String kind;

		public WatchEndpoint(IOpenShiftWatchListener listener, String kind) {
			this.listener = listener;
			this.kind = kind;
		}
		
		public void setResources(List<IResource> resources) {
			this.resources = resources;
		}
		
		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			LOGGER.debug("WatchSocket closed for kind {}", kind);
			getSession().close(statusCode, reason);
			super.onWebSocketClose(statusCode, reason);
			listener.disconnected();
		}

		@Override
		public void onWebSocketConnect(Session session) {
			LOGGER.debug("WatchSocket connected {}", kind);
			super.onWebSocketConnect(session);
			listener.connected(resources);

		}

		@Override
		public void onWebSocketError(Throwable err) {
			LOGGER.debug("WatchSocket Error for kind " + kind, err);
			listener.error(createOpenShiftException("WatchSocket Error", err));
		}

		@Override
		public void onWebSocketText(String message) {
			LOGGER.debug(message);
			KubernetesResource payload = factory.create(message);
			ModelNode node = payload.getNode();
			IOpenShiftWatchListener.ChangeType event = new ChangeType(node.get("type").asString());
			IResource resource = factory.create(node.get("object").toJSONString(true));
			if(StringUtils.isEmpty(resource.getKind())) {
				LOGGER.error("Unable to determine resource kind from: " + node.get("object").toJSONString(false));
			}
			listener.received(resource, event);
		}
	}
	
	public IWatcher watch(Collection<String> kinds, String namespace, IOpenShiftWatchListener listener) {
		try {
			ClientUpgradeRequest request = newRequest(this.client.getAuthorizationStrategy().getToken());	
			for (String kind : kinds) {
				WatchEndpoint socket = new WatchEndpoint(listener, kind);
				final String resourceVersion = getResourceVersion(kind, namespace, socket);
				
				final String endpoint = new URLBuilder(baseUrl, typeMappings)
						.kind(kind)
						.namespace(namespace)
						.watch()
						.addParmeter("resourceVersion", resourceVersion)
						.websocket();
				connect(socket, endpoint, request);
			}
		} catch (Exception e) {
			throw createOpenShiftException(String.format("Could not watch resources in namespace %s: %s", namespace, e.getMessage()), e);
		}
		return this;
	}
	
	private void connect(WatchEndpoint socket, String endpoint, ClientUpgradeRequest request) throws Exception {
		start();
		if(status.get() == Status.Starting) {
			isStarted.await(lockTimeout, TimeUnit.MILLISECONDS);
		}
		wsClient.connect(socket, new URI(endpoint), request);
	}
	
	public void start() {
		if(status.get().equals(Status.Started) || status.get().equals(Status.Starting)) {
			return;
		}
		try {
			wsClient.start();
		} catch (Exception e) {
			throw createOpenShiftException(String.format("Could not start watchClient"),e);
		}
	}
	
	@Override
	public void stop(){
		if(status.get().equals(Status.Stopping) || status.get().equals(Status.Stopped)) {
			return;
		}
		try {
			wsClient.stop();
		} catch (Exception e) {
			LOGGER.debug("Unable to stop the watch client",e);
		}
	}


	
	private ClientUpgradeRequest newRequest(final String token) {
		ClientUpgradeRequest request = new ClientUpgradeRequest();
		request.setHeader("Origin", baseUrl.toString());
		request.setHeader("User-Agent", "openshift-restclient-java");
		request.setHeader("Authorization", "Bearer " + token);	
		return request;
	}
	
	private static WebSocketClient newWebSocketClient() {
		SslContextFactory factory = new SslContextFactory();
		factory.setTrustAll(true);
		WebSocketClient client = new WebSocketClient(factory);
		return client;
	}
	
	private String getResourceVersion(String kind, String namespace, WatchEndpoint endpoint) throws Exception{
		IList list = client.get(kind, namespace);
		Collection<IResource> items = list.getItems();
		List<IResource> resources = new ArrayList<>(items.size());
		resources.addAll(items);
		endpoint.setResources(resources);
		return list.getMetadata().get("resourceVersion");
	}
	
	private OpenShiftException createOpenShiftException(String message, Throwable e) {
		LOGGER.debug(message, e);
		int responseCode = 0;
		if(e instanceof UpgradeException) {
			UpgradeException ex = (UpgradeException)e;
			responseCode = ex.getResponseStatusCode();
		}
		switch(responseCode) {
		case STATUS_FORBIDDEN:
			return new ResourceForbiddenException("Resource Forbidden", e);
		case STATUS_UNAUTHORIZED:
			return new com.openshift.restclient.authorization.UnauthorizedException(client.getAuthorizationDetails(this.baseUrl.toString()));
		default:
			return new OpenShiftException(e, message);
		}
	}
	
}
