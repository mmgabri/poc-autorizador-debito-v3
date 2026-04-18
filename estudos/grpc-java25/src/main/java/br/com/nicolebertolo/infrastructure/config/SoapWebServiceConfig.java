package br.com.nicolebertolo.infrastructure.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.xml.xsd.SimpleXsdSchema;

@EnableWs
@Configuration
@ComponentScan(basePackages = "br.com.nicolebertolo.infrastructure.adapter.inbound.soap")
public class SoapWebServiceConfig {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext context) {
        var servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(context);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "listagem")
    public org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition defaultWsdl11Definition(
            org.springframework.xml.xsd.SimpleXsdSchema listingsSchema) {

        var wsdl = new org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition();
        wsdl.setPortTypeName("ListingPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("http://nicolebertolo.com.br/listings");
        wsdl.setSchema(listingsSchema);
        return wsdl;
    }


    @Bean
    public org.springframework.ws.server.endpoint.mapping.PayloadRootAnnotationMethodEndpointMapping endpointMapping() {
        return new org.springframework.ws.server.endpoint.mapping.PayloadRootAnnotationMethodEndpointMapping();
    }

    @Bean
    public SimpleXsdSchema listingsSchema() {
        var resource = new ClassPathResource("listagem.xsd");
        System.out.println(">> Loading XSD from: " + resource.exists());
        return new SimpleXsdSchema(resource);
    }

}