package fr.smile.alfresco.graphql.integrationtest;

import java.io.File;
import java.time.Duration;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	GraphQlServletIT.class
})
public class DockerSuiteIT {

	private static final String SERVICE_NAME = "graphql-acs";
	private static final int SERVICE_PORT = 8080;

	@ClassRule
	public static DockerComposeContainer<?> environment = new DockerComposeContainer<>(
			new File("target/classes/docker/docker-compose.yml"))
				.withExposedService(SERVICE_NAME, SERVICE_PORT, 
						Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(120)))
				.withTailChildContainers(true)
//				.withLocalCompose(true)
				;

	@BeforeClass
	public static void init() {
		System.setProperty("acs.endpoint.path", 
				"http://" + environment.getServiceHost(SERVICE_NAME, SERVICE_PORT) 
				+ ":" + environment.getServicePort(SERVICE_NAME, SERVICE_PORT) 
				+ "/alfresco");
	}
}
