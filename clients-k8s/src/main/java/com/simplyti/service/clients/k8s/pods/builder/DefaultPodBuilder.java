package com.simplyti.service.clients.k8s.pods.builder;

import java.util.ArrayList;
import java.util.List;

import com.simplyti.service.api.serializer.json.Json;
import com.simplyti.service.clients.http.HttpClient;
import com.simplyti.service.clients.k8s.K8sAPI;
import com.simplyti.service.clients.k8s.common.Metadata;
import com.simplyti.service.clients.k8s.common.builder.AbstractK8sResourceBuilder;
import com.simplyti.service.clients.k8s.pods.domain.Container;
import com.simplyti.service.clients.k8s.pods.domain.ImagePullSecret;
import com.simplyti.service.clients.k8s.pods.domain.Pod;
import com.simplyti.service.clients.k8s.pods.domain.PodSpec;
import com.simplyti.service.clients.k8s.pods.domain.RestartPolicy;
import com.simplyti.service.clients.k8s.pods.domain.Volume;

public class DefaultPodBuilder extends AbstractK8sResourceBuilder<PodBuilder,Pod> implements PodBuilder, ContainerHolder, VolumeHolder {
	
	public static final String KIND = "Pod";
	
	private List<Container> containers;
	private List<ImagePullSecret> imagePullSecrets;
	private List<Volume> volumes;
	private RestartPolicy restartPolicy;
	private Integer terminationGracePeriodSeconds;
	private String schedulerName;

	public DefaultPodBuilder(HttpClient client,Json json,K8sAPI api,String namespace, String resource) {
		super(client,json,api,namespace,resource,Pod.class);
	}

	@Override
	protected Pod resource(K8sAPI api, Metadata metadata) {
		return new Pod(KIND, api.version(), metadata, new PodSpec(containers,restartPolicy,imagePullSecrets,terminationGracePeriodSeconds,volumes,schedulerName),null);
	}
	
	@Override
	public PodBuilder withRestartPolicy(RestartPolicy restartPolicy) {
		this.restartPolicy=restartPolicy;
		return this;
	}
	
	@Override
	public PodBuilder withTerminationGracePeriodSeconds(int terminationGraceSeconds) {
		this.terminationGracePeriodSeconds=terminationGraceSeconds;
		return this;
	}

	@Override
	public ContainerBuilder<PodBuilder> withContainer() {
		return new DefaultContainerBuilder<>(this,this);
	}
	
	@Override
	public VolumeBuilder withVolumes() {
		return new DefaultVolumeBuilder(this,this);
	}

	@Override
	public void addContainer(Container container) {
		if(containers==null) {
			this.containers=new ArrayList<>();
		}
		this.containers.add(container);
	}

	@Override
	public DefaultPodBuilder withImagePullSecret(String name) {
		if(imagePullSecrets==null) {
			this.imagePullSecrets=new ArrayList<>();
		}
		this.imagePullSecrets.add(new ImagePullSecret(name));
		return this;
	}

	@Override
	public void addVolume(Volume volume) {
		if(this.volumes==null) {
			this.volumes=new ArrayList<>();
		}
		this.volumes.add(volume);
	}

	@Override
	public PodBuilder withSchedulerName(String schedulerName) {
		this.schedulerName=schedulerName;
		return this;
	}

}
