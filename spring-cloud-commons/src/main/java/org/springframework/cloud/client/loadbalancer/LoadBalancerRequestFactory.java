/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.loadbalancer;

import java.util.List;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Creates {@link LoadBalancerRequest}s for {@link LoadBalancerInterceptor} and
 * {@link RetryLoadBalancerInterceptor}. Applies {@link LoadBalancerRequestTransformer}s
 * to the intercepted {@link HttpRequest}.
 *
 * @author William Tran
 * <p>
 * 负责拦截HttpRequest, 使用LoadBalancerRequestTransformer 创建LoadBalancerRequest
 */
public class LoadBalancerRequestFactory {

	private LoadBalancerClient loadBalancer;

	// TODO 这些transformer是干什么的????
	private List<LoadBalancerRequestTransformer> transformers;

	public LoadBalancerRequestFactory(LoadBalancerClient loadBalancer,
									  List<LoadBalancerRequestTransformer> transformers) {
		this.loadBalancer = loadBalancer;
		this.transformers = transformers;
	}

	public LoadBalancerRequestFactory(LoadBalancerClient loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	// 把httpRequest包装成LoadBalancerRequest
	public LoadBalancerRequest<ClientHttpResponse> createRequest(
			final HttpRequest request, final byte[] body,
			final ClientHttpRequestExecution execution) {
		return
				// 这就是一个loadBalancerRequest, request只有一个apply方法, 接收一个instance, 然后执行.
				instance
						// apply()方法
						-> {
					// 1. 把httpRequest包装起来, 包含loadBalancerClient和serviceInstance
					HttpRequest serviceRequest = new ServiceRequestWrapper(request, instance,
							this.loadBalancer);
					// 2. 把transformerList里的transformer依次应用到serviceRequestWrapper.
					if (this.transformers != null) {
						for (LoadBalancerRequestTransformer transformer : this.transformers) {
							serviceRequest = transformer.transformRequest(serviceRequest,
									instance);
						}
					}
					// 3. request真正执行: 去executor执行.
					return execution.execute(serviceRequest, body);
				};
	}

}
