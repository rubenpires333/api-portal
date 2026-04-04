package com.api_portal.backend.modules.api.domain.enums;

public enum ApiStatus {
    DRAFT,              // Rascunho - ainda não publicada
    PENDING_APPROVAL,   // Aguardando aprovação do administrador
    PUBLISHED,          // Publicada e disponível
    REJECTED,           // Rejeitada pelo administrador
    DEPRECATED,         // Descontinuada mas ainda funcional
    ARCHIVED            // Arquivada - não disponível
}
