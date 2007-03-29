/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.eip.packaging;

import java.util.ArrayList;
import java.util.List;

import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.packaging.Consumes;
import org.apache.servicemix.common.xbean.AbstractXBeanServiceUnitAnalyzer;
import org.apache.servicemix.eip.EIPEndpoint;
import org.apache.servicemix.eip.patterns.ContentBasedRouter;
import org.apache.servicemix.eip.patterns.MessageFilter;
import org.apache.servicemix.eip.patterns.Pipeline;
import org.apache.servicemix.eip.patterns.SplitAggregator;
import org.apache.servicemix.eip.patterns.StaticRecipientList;
import org.apache.servicemix.eip.patterns.StaticRoutingSlip;
import org.apache.servicemix.eip.patterns.WireTap;
import org.apache.servicemix.eip.patterns.XPathSplitter;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.eip.support.RoutingRule;

public class EipServiceUnitAnalyzer extends AbstractXBeanServiceUnitAnalyzer {

	protected List<Consumes> getConsumes(Endpoint endpoint) {
		if (endpoint instanceof ContentBasedRouter)
			return resolveContentBasedRouter((ContentBasedRouter) endpoint);
		if (endpoint instanceof MessageFilter)
			return resolveMessageFilter((MessageFilter) endpoint);
		if (endpoint instanceof Pipeline)
			return resolvePipeline((Pipeline) endpoint);
		if (endpoint instanceof SplitAggregator)
			return resolveSplitAggregator((SplitAggregator) endpoint);
		if (endpoint instanceof StaticRecipientList)
			return resolveStaticRecipientList((StaticRecipientList) endpoint);
		if (endpoint instanceof StaticRoutingSlip)
			return resolveStaticRoutingSlip((StaticRoutingSlip) endpoint);
		if (endpoint instanceof WireTap)
			return resolveWireTap((WireTap) endpoint);
		if (endpoint instanceof XPathSplitter)
			return resolveXPathSplitter((XPathSplitter) endpoint);
		return new ArrayList<Consumes>();
	}

	private List<Consumes> resolveXPathSplitter(XPathSplitter splitter) {
		return generateConsumesFromTarget(splitter.getTarget(), new ArrayList<Consumes>());
	}

	private List<Consumes> resolveWireTap(WireTap tap) {
		List<Consumes> consumes = new ArrayList<Consumes>();
		consumes = generateConsumesFromTarget(tap.getTarget(), consumes);
		consumes = generateConsumesFromTarget(tap.getInListener(), consumes);
		consumes = generateConsumesFromTarget(tap.getOutListener(), consumes);
		consumes = generateConsumesFromTarget(tap.getFaultListener(), consumes);
		return consumes;
	}

	private List<Consumes> resolveStaticRoutingSlip(StaticRoutingSlip slip) {
		List<Consumes> consumes = new ArrayList<Consumes>();
		for (int i = 0; i < slip.getTargets().length; i++) {
			consumes = generateConsumesFromTarget(slip.getTargets()[i],
					consumes);
		}
		return consumes;
	}

	private List<Consumes> resolveStaticRecipientList(StaticRecipientList list) {
		List<Consumes> consumes = new ArrayList<Consumes>();
		for (int i = 0; i < list.getRecipients().length; i++) {
			consumes = generateConsumesFromTarget(list.getRecipients()[i],
					consumes);
		}
		return consumes;
	}

	private List<Consumes> resolveSplitAggregator(SplitAggregator aggregator) {
		return generateConsumesFromTarget(aggregator.getTarget(),
				new ArrayList<Consumes>());
	}

	private List<Consumes> resolvePipeline(Pipeline pipeline) {
		List<Consumes> consumes = generateConsumesFromTarget(pipeline.getTarget(), new ArrayList<Consumes>());
		consumes = generateConsumesFromTarget(pipeline.getTransformer(), consumes);
		return consumes;
	}

	private List<Consumes> resolveMessageFilter(MessageFilter filter) {
		return generateConsumesFromTarget(filter.getTarget(), new ArrayList<Consumes>());
	}

	private List<Consumes> resolveContentBasedRouter(ContentBasedRouter router) {
		List<Consumes> consumes = new ArrayList<Consumes>();
		for (int i = 0; i < router.getRules().length; i++) {
			RoutingRule rule = router.getRules()[i];
			consumes = generateConsumesFromTarget(rule.getTarget(), consumes);
		}
		return consumes;
	}

	private List<Consumes> generateConsumesFromTarget(ExchangeTarget target, List<Consumes> consumes) {
		if (target != null) {
			Consumes consume = new Consumes();
			consume.setEndpointName(target.getEndpoint());
			consume.setServiceName(target.getService());
			consume.setInterfaceName(target.getInterface());
			consumes.add(consume);
		}
		return consumes;
	}

	protected String getXBeanFile() {
		return "xbean.xml";
	}

	protected boolean isValidEndpoint(Object bean) {
		return (bean instanceof EIPEndpoint);
	}

}
