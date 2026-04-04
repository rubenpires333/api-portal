package com.api_portal.backend.modules.user.service;

import com.api_portal.backend.modules.user.domain.Role;
import com.api_portal.backend.modules.user.domain.User;
import com.api_portal.backend.modules.user.domain.Address;
import com.api_portal.backend.modules.user.domain.Contact;
import com.api_portal.backend.modules.user.domain.repository.AddressRepository;
import com.api_portal.backend.modules.user.domain.repository.ContactRepository;
import com.api_portal.backend.modules.user.dto.UpdateUserRequest;
import com.api_portal.backend.modules.user.dto.UpdateProfileRequest;
import com.api_portal.backend.modules.user.dto.AddressRequest;
import com.api_portal.backend.modules.user.dto.AddressResponse;
import com.api_portal.backend.modules.user.dto.ContactRequest;
import com.api_portal.backend.modules.user.dto.ContactResponse;
import com.api_portal.backend.modules.user.dto.UserResponse;
import com.api_portal.backend.modules.user.repository.RoleRepository;
import com.api_portal.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;
    private final ContactRepository contactRepository;
    
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User createOrUpdateUser(String keycloakId, String email, String firstName, 
                                   String lastName, String username, Boolean emailVerified, 
                                   List<String> roleCodes) {
        log.info("=== Iniciando createOrUpdateUser para: {} ===", email);
        log.info("Roles do JWT: {}", roleCodes);
        
        // Buscar por keycloakId primeiro
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElse(null);
        
        // Se não encontrar por keycloakId, buscar por email (pode ser usuário antigo)
        if (user == null) {
            user = userRepository.findByEmail(email)
                .orElse(null);
            
            // Se encontrou por email, atualizar o keycloakId
            if (user != null) {
                log.info("Usuário encontrado por email, atualizando keycloakId");
                user.setKeycloakId(keycloakId);
            }
        }
        
        boolean isNewUser = (user == null);
        log.info("Usuário é novo? {}", isNewUser);
        
        if (isNewUser) {
            log.info("Criando novo usuário...");
            
            // Criar novo usuário
            user = User.builder()
                .keycloakId(keycloakId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .emailVerified(emailVerified != null ? emailVerified : false)
                .active(true)
                .roles(new HashSet<>())
                .build();
            
            log.info("Usuário criado em memória: {}", user.getEmail());
        } else {
            log.info("Atualizando usuário existente: {}", email);
            
            // Atualizar informações
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUsername(username);
            if (emailVerified != null) {
                user.setEmailVerified(emailVerified);
            }
        }
        
        // Sincronizar roles do JWT com o banco de dados
        if (roleCodes != null && !roleCodes.isEmpty()) {
            log.info("Sincronizando {} roles do JWT...", roleCodes.size());
            
            Set<Role> rolesToAssign = new HashSet<>();
            
            for (String roleCode : roleCodes) {
                // Ignorar roles do Keycloak que não são do nosso sistema
                if (roleCode.startsWith("default-") || roleCode.startsWith("offline_") || 
                    roleCode.equals("uma_authorization")) {
                    log.debug("Ignorando role do Keycloak: {}", roleCode);
                    continue;
                }
                
                Role role = roleRepository.findByCode(roleCode.toUpperCase())
                    .orElse(null);
                
                if (role != null) {
                    rolesToAssign.add(role);
                    log.info("✓ Role encontrada: {} ({})", role.getName(), role.getCode());
                } else {
                    log.warn("⚠ Role '{}' do JWT não encontrada no banco de dados", roleCode);
                }
            }
            
            if (!rolesToAssign.isEmpty()) {
                user.setRoles(rolesToAssign);
                log.info("✓ {} roles atribuídas ao usuário", rolesToAssign.size());
            } else {
                log.warn("⚠ Nenhuma role válida encontrada. Atribuindo CONSUMER por padrão...");
                
                // Fallback: atribuir CONSUMER se nenhuma role válida foi encontrada
                Role consumerRole = roleRepository.findByCode("CONSUMER").orElse(null);
                if (consumerRole != null) {
                    user.getRoles().add(consumerRole);
                    log.info("✓ Role CONSUMER atribuída como fallback");
                }
            }
        } else {
            log.warn("⚠ Nenhuma role no JWT. Atribuindo CONSUMER por padrão...");
            
            // Se não há roles no JWT, atribuir CONSUMER
            if (user.getRoles().isEmpty()) {
                Role consumerRole = roleRepository.findByCode("CONSUMER").orElse(null);
                if (consumerRole != null) {
                    user.getRoles().add(consumerRole);
                    log.info("✓ Role CONSUMER atribuída (sem roles no JWT)");
                }
            }
        }
        
        // Salvar usuário
        user = userRepository.save(user);
        log.info("✓ Usuário salvo no banco com ID: {}", user.getId());
        log.info("Total de roles no usuário após salvar: {}", user.getRoles().size());
        
        // Listar roles atribuídas
        if (!user.getRoles().isEmpty()) {
            user.getRoles().forEach(r -> 
                log.info("  - Role: {} ({})", r.getName(), r.getCode())
            );
            log.info("✓✓✓ SUCESSO: Roles sincronizadas para usuário: {}", email);
        } else {
            log.error("❌ ERRO: Usuário salvo mas sem roles!");
        }
        
        log.info("=== Finalizando createOrUpdateUser ===");
        return user;
    }
    
    @Transactional
    public void updateLastLogin(UUID id, String ipAddress) {
        log.info("Atualizando último login do usuário: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        
        userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com ID: " + id));
        
        return mapToResponse(user);
    }
    
    @Transactional(readOnly = true)
    public UserResponse getUserByKeycloakId(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        return mapToResponse(user);
    }
    
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByRole(String roleCode, Pageable pageable) {
        return userRepository.findByRoleCode(roleCode, pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional
    @CachePut(value = "users", key = "#id")
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.info("Atualizando usuário: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getCompany() != null) {
            user.setCompany(request.getCompany());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        if (request.getWebsite() != null) {
            user.setWebsite(request.getWebsite());
        }
        
        user = userRepository.save(user);
        
        return mapToResponse(user);
    }
    
    @Transactional
    @CachePut(value = "users", key = "#id")
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
        log.info("Atualizando perfil completo do usuário: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getCompany() != null) {
            user.setCompany(request.getCompany());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        if (request.getWebsite() != null) {
            user.setWebsite(request.getWebsite());
        }
        if (request.getNif() != null) {
            user.setNif(request.getNif());
        }
        if (request.getDocumentType() != null) {
            user.setDocumentType(request.getDocumentType());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        
        user = userRepository.save(user);
        
        return mapToResponse(user);
    }
    
    // Address methods
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(UUID userId) {
        return addressRepository.findByUserId(userId).stream()
            .map(this::mapToAddressResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public AddressResponse addAddress(UUID userId, AddressRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        Address address = Address.builder()
            .user(user)
            .street(request.getStreet())
            .number(request.getNumber())
            .complement(request.getComplement())
            .neighborhood(request.getNeighborhood())
            .city(request.getCity())
            .state(request.getState())
            .postalCode(request.getPostalCode())
            .country(request.getCountry() != null ? request.getCountry() : "Cabo Verde")
            .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
            .build();
        
        address = addressRepository.save(address);
        return mapToAddressResponse(address);
    }
    
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Endereço não pertence ao usuário");
        }
        
        if (request.getStreet() != null) {
            address.setStreet(request.getStreet());
        }
        if (request.getNumber() != null) {
            address.setNumber(request.getNumber());
        }
        if (request.getComplement() != null) {
            address.setComplement(request.getComplement());
        }
        if (request.getNeighborhood() != null) {
            address.setNeighborhood(request.getNeighborhood());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getState() != null) {
            address.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            address.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }
        if (request.getIsPrimary() != null) {
            address.setIsPrimary(request.getIsPrimary());
        }
        
        address = addressRepository.save(address);
        return mapToAddressResponse(address);
    }
    
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Endereço não pertence ao usuário");
        }
        
        addressRepository.delete(address);
    }
    
    // Contact methods
    @Transactional(readOnly = true)
    public List<ContactResponse> getUserContacts(UUID userId) {
        return contactRepository.findByUserId(userId).stream()
            .map(this::mapToContactResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public ContactResponse addContact(UUID userId, ContactRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        Contact contact = Contact.builder()
            .user(user)
            .type(request.getType())
            .value(request.getValue())
            .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
            .isVerified(false)
            .build();
        
        contact = contactRepository.save(contact);
        return mapToContactResponse(contact);
    }
    
    @Transactional
    public ContactResponse updateContact(UUID userId, UUID contactId, ContactRequest request) {
        Contact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new RuntimeException("Contato não encontrado"));
        
        if (!contact.getUser().getId().equals(userId)) {
            throw new RuntimeException("Contato não pertence ao usuário");
        }
        
        if (request.getType() != null) {
            contact.setType(request.getType());
        }
        if (request.getValue() != null) {
            contact.setValue(request.getValue());
        }
        if (request.getIsPrimary() != null) {
            contact.setIsPrimary(request.getIsPrimary());
        }
        
        contact = contactRepository.save(contact);
        return mapToContactResponse(contact);
    }
    
    @Transactional
    public void deleteContact(UUID userId, UUID contactId) {
        Contact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new RuntimeException("Contato não encontrado"));
        
        if (!contact.getUser().getId().equals(userId)) {
            throw new RuntimeException("Contato não pertence ao usuário");
        }
        
        contactRepository.delete(contact);
    }
    
    private AddressResponse mapToAddressResponse(Address address) {
        return AddressResponse.builder()
            .id(address.getId())
            .street(address.getStreet())
            .number(address.getNumber())
            .complement(address.getComplement())
            .neighborhood(address.getNeighborhood())
            .city(address.getCity())
            .state(address.getState())
            .postalCode(address.getPostalCode())
            .country(address.getCountry())
            .isPrimary(address.getIsPrimary())
            .build();
    }
    
    private ContactResponse mapToContactResponse(Contact contact) {
        return ContactResponse.builder()
            .id(contact.getId())
            .type(contact.getType())
            .value(contact.getValue())
            .isPrimary(contact.getIsPrimary())
            .isVerified(contact.getIsVerified())
            .build();
    }
    
    @Transactional
    @CachePut(value = "users", key = "#userId")
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse assignRoles(UUID userId, Set<UUID> roleIds) {
        log.info("Atribuindo roles ao usuário: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        Set<Role> roles = new HashSet<>();
        for (UUID roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role não encontrada: " + roleId));
            roles.add(role);
        }
        
        user.setRoles(roles);
        user = userRepository.save(user);
        
        return mapToResponse(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void deactivateUser(UUID id) {
        log.info("Desativando usuário: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        user.setActive(false);
        userRepository.save(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void activateUser(UUID id) {
        log.info("Ativando usuário: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        user.setActive(true);
        userRepository.save(user);
    }
    
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .keycloakId(user.getKeycloakId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .fullName(user.getFullName())
            .username(user.getUsername())
            .phoneNumber(user.getPhoneNumber())
            .avatarUrl(user.getAvatarUrl())
            .emailVerified(user.getEmailVerified())
            .active(user.getActive())
            .lastLoginAt(user.getLastLoginAt())
            .lastLoginIp(user.getLastLoginIp())
            .roles(user.getRoles().stream()
                .map(role -> UserResponse.RoleInfo.builder()
                    .id(role.getId())
                    .name(role.getName())
                    .code(role.getCode())
                    .description(role.getDescription())
                    .build())
                .collect(Collectors.toSet()))
            .bio(user.getBio())
            .company(user.getCompany())
            .location(user.getLocation())
            .website(user.getWebsite())
            .nif(user.getNif())
            .documentType(user.getDocumentType())
            .birthDate(user.getBirthDate())
            .gender(user.getGender())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .createdBy(user.getCreatedBy())
            .lastModifiedBy(user.getLastModifiedBy())
            .build();
    }
}
