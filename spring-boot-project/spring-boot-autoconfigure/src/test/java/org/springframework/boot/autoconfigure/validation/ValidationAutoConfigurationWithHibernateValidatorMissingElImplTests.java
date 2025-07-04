/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.autoconfigure.validation;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ValidationAutoConfiguration} when Hibernate validator is present but no
 * EL implementation is available.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions({ "tomcat-embed-el-*.jar", "el-api-*.jar" })
class ValidationAutoConfigurationWithHibernateValidatorMissingElImplTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class));

	@Test
	void missingElDependencyIsTolerated() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Validator.class);
			assertThat(context).hasSingleBean(MethodValidationPostProcessor.class);
		});
	}

}
