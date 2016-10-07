/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.openshift.internal.restclient.model.build;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jboss.dmr.ModelNode;

import com.openshift.internal.restclient.model.BuildConfig;
import com.openshift.internal.util.JBossDmrExtentions;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IEnvironmentVariable;
import com.openshift.restclient.model.build.BuildTriggerType;
import com.openshift.restclient.model.build.IBuildConfigBuilder;


public class BuildConfigBuilder implements IBuildConfigBuilder {
	
	private SourceStrategyBuilder sourceStrategyBuilder;
	private GitSourceBuilder gitSourceBuilder;
	private String imageStreamTagOutput;
	private boolean buildOnConfigChange;
	private boolean buildOnImageChange;
	private boolean buildOnSourceChange;
	private final IClient client;
	private String name;
	private String namespace;
	
	public BuildConfigBuilder(IClient client) {
		this.client = client;
	}
	
	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public String getName() {
		return BuildConfigBuilder.class.getSimpleName();
	}
	
	@Override
	public IBuildConfigBuilder named(String name) {
		this.name = name;
		return this;
	}

	@Override
	public IBuildConfigBuilder inNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	@Override
	public IBuildConfig build() {
		BuildConfig bc = client.getResourceFactory().stub(ResourceKind.BUILD_CONFIG, this.name, this.namespace);
		
		if(sourceStrategyBuilder != null) {
			bc.setBuildStrategy(sourceStrategyBuilder.build(bc.getPropertyKeys()));
		}
		if(gitSourceBuilder != null) {
			bc.setBuildSource(gitSourceBuilder.build());
		}


		//TODO move into bc
		ModelNode node = bc.getNode();
		JBossDmrExtentions.set(node, Collections.emptyMap(), "spec.output.to.kind", ResourceKind.IMAGE_STREAM_TAG);
		JBossDmrExtentions.set(node, Collections.emptyMap(), "spec.output.to.name", imageStreamTagOutput);
		
		bc.addBuildTrigger(new WebhookTrigger(BuildTriggerType.GENERIC, UUID.randomUUID().toString(), null));
		if(buildOnImageChange) {
			bc.addBuildTrigger(new ImageChangeTrigger(BuildTriggerType.IMAGE_CHANGE, null, null, null));
		}
		if(buildOnConfigChange) {
			bc.addBuildTrigger(new ImageChangeTrigger(BuildTriggerType.CONFIG_CHANGE,null, null, null));
		}
		if(buildOnSourceChange) {
			bc.addBuildTrigger(new WebhookTrigger(BuildTriggerType.GITHUB, UUID.randomUUID().toString(), null));
		}
		
		return bc;
	}
	
	@Override
	public IBuildConfigBuilder buildOnSourceChange(boolean onSourceChange) {
		this.buildOnSourceChange = onSourceChange;
		return this;
	}

	@Override
	public IBuildConfigBuilder buildOnImageChange(boolean onImageChange) {
		this.buildOnImageChange = onImageChange;
		return this;
	}

	@Override
	public IBuildConfigBuilder buildOnConfigChange(boolean onConfigChange) {
		this.buildOnConfigChange = onConfigChange;
		return this;
	}

	@Override
	public IBuildConfigBuilder toImageStreamTag(String tag) {
		imageStreamTagOutput = tag;
		return this;
	}

	@Override
	public ISourceStrategyBuilder usingSourceStrategy() {
		sourceStrategyBuilder = new SourceStrategyBuilder(this);
		return sourceStrategyBuilder; 
	}
	
	@Override
	public IGitSourceBuilder fromGitSource() {
		gitSourceBuilder = new GitSourceBuilder(this);
		return gitSourceBuilder;
	}

	class GitSourceBuilder implements IGitSourceBuilder {
		
		private IBuildConfigBuilder builder;
		private String url;
		private String ref;
		private String contextDir;
		
		GitSourceBuilder(IBuildConfigBuilder builder){
			this.builder = builder;
		}

		private GitBuildSource build() {
			return new GitBuildSource(url, ref, contextDir);
		}
		
		@Override
		public IBuildConfigBuilder end() {
			return builder;
		}

		@Override
		public IGitSourceBuilder fromGitUrl(String url) {
			this.url = url;
			return this;
		}

		@Override
		public IGitSourceBuilder usingGitReference(String ref) {
			this.ref = ref;
			return this;
		}

		@Override
		public IGitSourceBuilder inContextDir(String contextDir) {
			this.contextDir = contextDir;
			return this;
		}
		
	}


	class SourceStrategyBuilder implements ISourceStrategyBuilder{
		
		private IBuildConfigBuilder builder;
		private List<IEnvironmentVariable> envVars;
		private String namespace;
		private String tag;
		private String fromKind;
		
		SourceStrategyBuilder(IBuildConfigBuilder builder){
			this.builder = builder;
		}
		
		public SourceBuildStrategy build(Map<String, String[]> overrides) {
			SourceBuildStrategy strategy = new SourceBuildStrategy(new ModelNode(), overrides);
			strategy.setEnvVars(envVars);
			strategy.setFromNamespace(namespace);
			strategy.setImage(new DockerImageURI(tag));
			strategy.setFromKind(fromKind);
			return strategy;
		}
		
		@Override
		public IBuildConfigBuilder end() {
			return builder;
		}

		@Override
		public ISourceStrategyBuilder fromImageStreamTag(String tag) {
			this.tag = tag;
			this.fromKind = ResourceKind.IMAGE_STREAM_TAG;
			return this;
		}

		@Override
		public ISourceStrategyBuilder inNamespace(String namespace) {
			this.namespace = namespace;
			return this;
		}
		
		
		@Override
		public ISourceStrategyBuilder withEnvVars(List<IEnvironmentVariable> envVars) {
			this.envVars = envVars;
			return this;
		}

		@Override
		public ISourceStrategyBuilder fromDockerImage(String tag) {
			this.tag = tag;
			this.fromKind = "DockerImage";
			return this;
		}
		
		

	}

}
