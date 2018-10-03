package gov.nsf.psm.documentcompliance;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import gov.nsf.psm.documentcompliance.compliance.common.utility.Constants;

@SpringBootApplication
public class DocumentComplianceServiceApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DocumentComplianceServiceApplication.class);
    }

    public static void main(String[] args) {
        setEmbeddedContainerEnvironmentProperties();
        SpringApplication.run(DocumentComplianceServiceApplication.class, args);
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        setExternalContainerEnvironmentProperties();
        super.onStartup(servletContext);
    }

    private static void setEmbeddedContainerEnvironmentProperties() {
        setEnvironmentProperties();
        System.setProperty("server.context-path", Constants.CONTEXT_DEFAULT_NAME);
    }

    private static void setExternalContainerEnvironmentProperties() {
        setEnvironmentProperties();
    }

    private static void setEnvironmentProperties() {
        System.setProperty("spring.config.name", Constants.CONFIG_FILE_DEFAULT_NAME);
    }

    @Bean
    EmbeddedServletContainerCustomizer containerCustomizer() {
        return (ConfigurableEmbeddedServletContainer container) -> {
            if (container instanceof TomcatEmbeddedServletContainerFactory) {
                TomcatEmbeddedServletContainerFactory tomcat = (TomcatEmbeddedServletContainerFactory) container;
                tomcat.addConnectorCustomizers((connector) -> {
                    connector.setMaxPostSize(Constants.MAX_FILE_SIZE_BYTES);
                });
            }
        };
    }

}
