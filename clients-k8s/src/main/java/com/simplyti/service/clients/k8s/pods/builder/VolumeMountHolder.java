package com.simplyti.service.clients.k8s.pods.builder;

import com.simplyti.service.clients.k8s.pods.domain.VolumeMount;

public interface VolumeMountHolder {

	void addVolumeMount(VolumeMount volumeMount);

}
