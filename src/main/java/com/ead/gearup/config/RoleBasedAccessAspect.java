package com.ead.gearup.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.ead.gearup.exception.AccessDeniedException;
import com.ead.gearup.service.auth.RoleBasedAccessService;
import com.ead.gearup.validation.RequiresRole;

import lombok.RequiredArgsConstructor;

@Component
@Aspect
@RequiredArgsConstructor
public class RoleBasedAccessAspect {

    private final RoleBasedAccessService roleBasedAccessService;

    @Around("@annotation(requiresRole)")
    public Object checkRoleAccess(ProceedingJoinPoint joinPoint, RequiresRole requiresRole) throws Throwable {

        if (!roleBasedAccessService.hasAnyRole(requiresRole.value())) {
            throw new AccessDeniedException(requiresRole.message());
        }

        return joinPoint.proceed();
    }
}
