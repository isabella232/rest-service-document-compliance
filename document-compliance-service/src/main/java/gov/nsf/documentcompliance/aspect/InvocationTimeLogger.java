package gov.nsf.documentcompliance.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class InvocationTimeLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvocationTimeLogger.class);

    @Around("execution(* gov.nsf.psm.documentcompliance.*.*.*(..))")
    public Object time(final ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        Object value;

        try {
            value = proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            throw throwable;
        } finally {
            long duration = System.currentTimeMillis() - start;

            LOGGER.debug("{}.{} took {} ms", proceedingJoinPoint.getSignature().getDeclaringType().getSimpleName(),
                    proceedingJoinPoint.getSignature().getName(), duration);
        }

        return value;
    }

}
