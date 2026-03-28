package com.api_portal.backend.shared.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para controlar acesso baseado em permissões.
 * 
 * Uso:
 * @RequiresPermission("api.create")
 * @RequiresPermission(value = {"api.create", "api.update"}, requireAll = false)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    
    /**
     * Código(s) da(s) permissão(ões) necessária(s).
     * Formato: "resource.action" (ex: "api.create", "user.read")
     */
    String[] value();
    
    /**
     * Se true, o usuário precisa ter TODAS as permissões listadas.
     * Se false, o usuário precisa ter PELO MENOS UMA das permissões.
     * Default: true (requer todas)
     */
    boolean requireAll() default true;
    
    /**
     * Mensagem de erro customizada quando acesso é negado.
     */
    String message() default "Acesso negado: permissão insuficiente";
}
