package gov.nsf.psm.documentcompliance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("gov.nsf.psm.documentcompliance.controller"))
                .paths(PathSelectors.any()).build().apiInfo(apiInfo()).useDefaultResponseMessages(false);
    }

    private static ApiInfo apiInfo() {
        return new ApiInfo("Document Compliance Service API",
                "Documentation describing RESTful endpoints hosted by the Document Compliance Service",
                "v 0.0.1-SNAPSHOT", null, new Contact("PSM Development Team", null, "psm-devteam@nsf.gov"), null, null);
    }
}
