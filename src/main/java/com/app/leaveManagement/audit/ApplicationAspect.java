package com.app.leaveManagement.audit;

import com.app.leaveManagement.entity.AuditLog;
import com.app.leaveManagement.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationAspect {

    private final AuditLogRepository auditLogRepository;

    // All methods in any service implementation class
    @Pointcut("execution(* com.app.leaveManagement.service.impl.*.*(..))")
    public void serviceLayer() {}

    // Only methods annotated with @Auditable
    @Pointcut("@annotation(com.app.leaveManagement.audit.Auditable)")
    public void auditableMethods() {}

    // @Around – execution time logging
    @Around("serviceLayer()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("[PERF] {}.{}() completed in {} ms", className, methodName, duration);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("[PERF] {}.{}() failed after {} ms", className, methodName, duration);
            throw ex;
        }
    }

    // @AfterThrowing – exception logging
    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logServiceException(JoinPoint joinPoint, Throwable ex) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.error("[EXCEPTION] {}.{}() threw: {} — Message: {}",
                className, methodName,
                ex.getClass().getSimpleName(),
                ex.getMessage());
    }

    // @After – audit logging for @Auditable methods
    @After("auditableMethods()")
    public void auditMethodCall(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Auditable auditable = method.getAnnotation(Auditable.class);

            String performedBy = getCurrentUsername();

            AuditLog auditLog = AuditLog.builder()
                    .entityType(auditable.entityType())
                    .action(auditable.action())
                    .performedBy(performedBy)
                    .description(
                        String.format("Method %s.%s() executed by %s",
                            joinPoint.getTarget().getClass().getSimpleName(),
                            joinPoint.getSignature().getName(),
                            performedBy)
                    )
                    .build();

            auditLogRepository.save(auditLog);

            log.info("[AUDIT] action={}, entityType={}, performedBy={}",
                    auditable.action(), auditable.entityType(), performedBy);

        } catch (Exception e) {
            // Never let audit logging break the main flow
            log.error("[AUDIT] Failed to save audit log: {}", e.getMessage());
        }
    }

    // @Before – log method entry with args
    @Before("serviceLayer()")
    public void logMethodEntry(JoinPoint joinPoint) {
        if (log.isDebugEnabled()) {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            log.debug("[ENTRY] {}.{}() called with {} arg(s)", className, methodName, args.length);
        }
    }

    // Helper – get currently authenticated user's email
    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve authenticated user for audit log");
        }
        return "SYSTEM";
    }
}