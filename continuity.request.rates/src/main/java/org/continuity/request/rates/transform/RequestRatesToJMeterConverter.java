package org.continuity.request.rates.transform;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.Controller;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.ThroughputController;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.control.gui.ThroughputControllerGui;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.gui.CookiePanel;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.threads.SetupThreadGroup;
import org.apache.jmeter.threads.gui.SetupThreadGroupGui;
import org.apache.jmeter.timers.ConstantThroughputTimer;
import org.apache.jmeter.visualizers.ViewResultsFullVisualizer;
import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.request.rates.model.RequestFrequency;
import org.continuity.request.rates.model.RequestRatesModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestRatesToJMeterConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestRatesToJMeterConverter.class);

	public JMeterTestPlanBundle convertToLoadTest(RequestRatesModel model) {
		ListedHashTree testPlanTree = new ListedHashTree(createTestPlan());
		ListedHashTree threadGroupTree = (ListedHashTree) testPlanTree.add(createThreadGroup(model.getRequestsPerMinute()));

		threadGroupTree.add(createHeaderManager());
		threadGroupTree.add(createCookieManager());
		threadGroupTree.add(createUserDefinedVariables());

		for (RequestFrequency frequency : model.getMix()) {
			ListedHashTree throughputControllerTree = (ListedHashTree) threadGroupTree.add(createThroughputController(frequency));

			if (frequency.getEndpoint() instanceof HttpEndpoint) {
				throughputControllerTree.add(createHttpSampler((HttpEndpoint) frequency.getEndpoint()));
			} else {
				LOGGER.error("Endpoint type {} of endpoint {} is not supported!", frequency.getEndpoint().getClass(), frequency.getEndpoint().getId());
			}
		}

		threadGroupTree.add(createTimer(model.getRequestsPerMinute()));

		threadGroupTree.add(createViewResultsTree());

		return new JMeterTestPlanBundle(testPlanTree, Collections.emptyMap());
	}

	private TestPlan createTestPlan() {
		TestPlanGui gui = new TestPlanGui();
		return (TestPlan) gui.createTestElement();
	}

	private SetupThreadGroup createThreadGroup(double requestRatePerMinute) {
		SetupThreadGroupGui gui = new SetupThreadGroupGui();
		SetupThreadGroup threadGroup = (SetupThreadGroup) gui.createTestElement();

		threadGroup.setName("Thread Group");

		int numUsers = (int) Math.ceil(requestRatePerMinute / 10.0);

		threadGroup.setNumThreads(numUsers);
		threadGroup.setRampUp(numUsers);
		threadGroup.setScheduler(true);
		threadGroup.setDuration(60);
		threadGroup.setDelay(0);
		threadGroup.setStartTime(0);
		threadGroup.setEndTime(0);

		Controller mainController = threadGroup.getSamplerController();

		if (mainController instanceof LoopController) {
			// Sets Loop Count: Forever [CHECK]
			((LoopController) mainController).setLoops(-1);
		}

		return threadGroup;
	}

	private HeaderManager createHeaderManager() {
		HeaderPanel gui = new HeaderPanel();

		return (HeaderManager) gui.createTestElement();
	}

	private CookieManager createCookieManager() {
		CookiePanel gui = new CookiePanel();

		return (CookieManager) gui.createTestElement();
	}

	private Arguments createUserDefinedVariables() {
		ArgumentsPanel gui = new ArgumentsPanel("Test Data");

		return (Arguments) gui.createTestElement();
	}

	private ThroughputController createThroughputController(RequestFrequency frequency) {
		ThroughputControllerGui gui = new ThroughputControllerGui();
		ThroughputController controller = (ThroughputController) gui.createTestElement();

		controller.setName("TC: " + frequency.getEndpoint().getId());
		controller.setStyle(1);
		controller.setPercentThroughput((float) (frequency.getFreq() * 100.0));
		controller.setPerThread(false);

		return controller;
	}

	private HTTPSamplerProxy createHttpSampler(HttpEndpoint endpoint) {
		HTTPSamplerProxy sampler = new HTTPSamplerProxy();

		sampler.setName(endpoint.getId());

		setIfNotNull(sampler::setDomain, endpoint.getDomain());
		sampler.setPort(endpoint.getPort() == null ? 80 : Integer.parseInt(endpoint.getPort()));
		setIfNotNull(sampler::setProtocol, endpoint.getProtocol());
		setIfNotNull(sampler::setMethod, endpoint.getMethod());
		setIfNotNull(sampler::setPath, endpoint.getPath());

		if (!Objects.equals(HttpEndpoint.DEFAULT_ENCODING, endpoint.getEncoding())) {
			setIfNotNull(sampler::setContentEncoding, endpoint.getEncoding());
		}

		sampler.setAutoRedirects(false);
		sampler.setFollowRedirects(true);
		sampler.setUseKeepAlive(true);
		sampler.setDoBrowserCompatibleMultipart(false);

		sampler.setEnabled(true);

		sampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
		sampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());

		return sampler;
	}

	private <T> void setIfNotNull(Consumer<T> setter, T value) {
		if (value != null) {
			setter.accept(value);
		}
	}

	private ConstantThroughputTimer createTimer(double requestsPerMinute) {
		ConstantThroughputTimer timer = new ConstantThroughputTimer();

		timer.setName("Constant Throughput Timer");

		timer.setProperty("throughput", Double.toString(requestsPerMinute));
		timer.setProperty("calcMode", 2);
		timer.setThroughput(requestsPerMinute);
		timer.setCalcMode(4); // ConstantThroughputTimer.Mode.AllActiveThreadsInCurrentThreadGroup_Shared

		timer.setEnabled(true);

		timer.setProperty(TestElement.TEST_CLASS, ConstantThroughputTimer.class.getName());
		timer.setProperty(TestElement.GUI_CLASS, TestBeanGUI.class.getName());
		return timer;
	}

	private ResultCollector createViewResultsTree() {
		ResultCollector resultCollector = new ResultCollector();
		resultCollector.setName("View Results Tree");

		resultCollector.setProperty(new StringProperty(TestElement.GUI_CLASS, ViewResultsFullVisualizer.class.getName()));
		resultCollector.setProperty(new StringProperty(TestElement.TEST_CLASS, ResultCollector.class.getName()));

		return resultCollector;
	}

}
