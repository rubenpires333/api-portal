package com.api_portal.backend.shared.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {
    
    private final PermissionService permissionService;
    
    /**
     * Intercepta métodos anotados com @RequiresPermission e valida permissões.
     */
    @Around("@annotation(com.api_portal.backend.shared.security.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
        
        if (annotation == null) {
            // Se não tem anotação no método, verificar na classe
            annotation = joinPoint.getTarget().getClass().getAnnotation(RequiresPermission.class);
        }
        
        if (annotation == null) {
            log.warn("Anotação @RequiresPermission não encontrada");
            return joinPoint.proceed();
        }
        
        String[] requiredPermissions = annotation.value();
        boolean requireAll = annotation.requireAll();
        String message = annotation.message();
        
        log.debug("Verificando permissões: {} (requireAll={})", 
            String.join(", ", requiredPermissions), requireAll);
        
        // SUPER_ADMIN tem acesso a tudo
        if (permissionService.isSuperAdmin()) {
            log.debug("Usuário é SUPER_ADMIN, acesso permitido");
            return joinPoint.proceed();
        }
        
        boolean hasAccess;
        
        if (requireAll) {
            // Usuário precisa ter TODAS as permissões
            hasAccess = permissionService.hasAllPermissions(requiredPermissions);
        } else {
            // Usuário precisa ter PELO MENOS UMA permissão
            hasAccess = permissionService.hasAnyPermission(requiredPermissions);
        }
        
        if (!hasAccess) {
            log.warn("Acesso negado ao método: {}.{} - Permissões necessárias: {}", 
                joinPoint.getTarget().getClass().getSimpleName(),
                method.getName(),
                String.join(", ", requiredPermissions));
            
            throw new AccessDeniedException(message);
        }
        
        log.debug("Acesso permitido ao método: {}.{}", 
            joinPoint.getTarget().getClass().getSimpleName(),
            method.getName());
        
        return joinPoint.proceed();
    }
    
    /**
     * Intercepta classes anotadas com @RequiresPermission.
     */
    @Around("@within(com.api_portal.backend.shared.security.RequiresPermission) && " +
            "!@annotation(com.api_portal.backend.shared.security.RequiresPermission)")
    public Object checkClassPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        RequiresPermission annotation = targetClass.getAnnotation(RequiresPermission.class);
        
        if (annotation == null) {
            return joinPoint.proceed();
        }
        
        String[] requiredPermissions = annotation.value();
        boolean requireAll = annotation.requireAll();
        String message = annotation.message();
        
        log.debug("Verificando permissões da classe: {} - Permissões: {}", 
            targetClass.getSimpleName(),
            String.join(", ", requiredPermissions));
        
        // SUPER_ADMIN tem acesso a tudo
        if (permissionService.isSuperAdmin()) {
            log.debug("Usuário é SUPER_ADMIN, acesso permitido");
            return joinPoint.proceed();
        }
        
        boolean hasAccess;
        
        if (requireAll) {
            hasAccess = permissionService.hasAllPermissions(requiredPermissions);
        } else {
            hasAccess = permissionService.hasAnyPermission(requiredPermissions);
        }
        
        if (!hasAccess) {
            log.warn("Acesso negado à classe: {} - Permissões necessárias: {}", 
                targetClass.getSimpleName(),
                String.join(", ", requiredPermissions));
            
            throw new AccessDeniedException(message);
        }
        
        return joinPoint.proceed();
    }
}
