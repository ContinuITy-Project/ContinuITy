package org.continuity.cli.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContext implements ApplicationContextAware {

	private static ApplicationContext context;

	public static <T> T getBean(Class<T> type) {
		return context.getBean(type);
	}

	public static <T> T getBean(Class<T> type, String name) {
		return context.getBean(name, type);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}

}
