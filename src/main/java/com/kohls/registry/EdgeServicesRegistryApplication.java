package com.kohls.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.kohls.registry.dao.Registry;
import com.kohls.registry.dao.RegistryItem;

@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class })
public class EdgeServicesRegistryApplication {

	public static void main(String[] args) {
		SpringApplication.run(EdgeServicesRegistryApplication.class, args);
	}

	@Bean
	@LoadBalanced
	public RestTemplate createRestTemplate() {
		return new RestTemplate();
	}

	@Bean
	public AlwaysSampler createSampler() {
		return new AlwaysSampler();
	}

	@Bean
	public EmbeddedServletContainerFactory servletContainer() {

		TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
		Connector ajpConnector = new Connector("AJP/1.3");
		ajpConnector.setProtocol("AJP/1.3");
		ajpConnector.setPort(9003);
		ajpConnector.setSecure(false);
		ajpConnector.setAllowTrace(false);
		ajpConnector.setScheme("http");
		tomcat.addAdditionalTomcatConnectors(ajpConnector);
		return tomcat;
	}
}

@RestController
class GetRegistryItems {

	@Autowired
	private RestTemplate rTemplate;

	@RequestMapping(method = RequestMethod.GET, path = "/{id}/name")
	public List<String> getRegistryItemNames(
			@PathVariable("id") String registryId) {
		ResponseEntity<RegistryData> response = rTemplate.getForEntity(
				"http://get-registry/" + registryId, RegistryData.class);

		return response.getBody().getRegistryItems().stream()
				.map(RegistryItem::getDescription).collect(Collectors.toList());
	}
}

class RegistryData {
	private Registry registry;
	private List<RegistryItem> registryItems = new ArrayList<>();

	public Registry getRegistry() {
		return registry;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public List<RegistryItem> getRegistryItems() {
		return registryItems;
	}

	public void addItems(RegistryItem item) {
		registryItems.add(item);
	}
}