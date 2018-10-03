package gov.nsf.psm.documentcompliance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import gov.nsf.psm.documentcompliance.service.DocumentComplianceService;
import gov.nsf.psm.documentcompliance.service.DocumentComplianceServiceImpl;

@Configuration
public class DocumentComplianceServiceConfig {

    @Bean
    @Primary
    public DocumentComplianceService pdfComplianceService() {
        return new DocumentComplianceServiceImpl();
    }

}
