# Problema: Lombok Não Está Gerando Getters/Setters

## Erro

```
cannot find symbol
  symbol:   method getName()
  location: variable request of type com.api_portal.backend.modules.api.dto.ApiCategoryRequest
```

## Causa

O Lombok não está sendo processado durante a compilação. O Maven não está executando o annotation processor do Lombok.

## Solução

### Opção 1: Verificar pom.xml (RECOMENDADO)

Verifique se o Lombok está configurado corretamente no `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Opção 2: Limpar e Recompilar

Execute os seguintes comandos:

```bash
# 1. Limpar completamente
mvn clean

# 2. Remover diretório target manualmente
rmdir /s /q target

# 3. Compilar novamente
mvn compile -DskipTests

# 4. Se ainda não funcionar, tente:
mvn clean install -DskipTests -U
```

### Opção 3: Delombok (Última Opção)

Se o Lombok continuar não funcionando, você pode "delombok" o código (gerar os getters/setters manualmente):

```bash
mvn lombok:delombok
```

Isso vai gerar o código Java puro sem anotações do Lombok.

## Verificação

Para verificar se o Lombok está funcionando, execute:

```bash
mvn clean compile -X | findstr "lombok"
```

Você deve ver algo como:
```
[DEBUG] Configuring mojo 'org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile'
[DEBUG] Processing annotation processor: lombok
```

## Solução Temporária

Se você precisa que a aplicação funcione AGORA e não pode esperar para resolver o Lombok, posso:

1. Remover as anotações do Lombok das classes problemáticas
2. Adicionar getters/setters manualmente
3. Isso vai fazer o código compilar imediatamente

**Deseja que eu faça isso?** (Responda SIM ou NÃO)

## Próximos Passos

1. Verifique o `pom.xml` para confirmar a configuração do Lombok
2. Execute `mvn clean compile -DskipTests`
3. Se ainda não funcionar, me avise e eu removo o Lombok das classes problemáticas


---

## ✅ PROBLEMA RESOLVIDO

**Data**: 28 de Março de 2026, 13:29

### O Que Foi Feito

1. **Adicionada versão do Lombok no `annotationProcessorPaths`**
   
   O problema era que o `maven-compiler-plugin` não tinha a versão do Lombok especificada no `annotationProcessorPaths`. Isso foi corrigido:

   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <configuration>
           <release>21</release>
           <annotationProcessorPaths>
               <path>
                   <groupId>org.projectlombok</groupId>
                   <artifactId>lombok</artifactId>
                   <version>${lombok.version}</version>  <!-- ADICIONADO -->
               </path>
           </annotationProcessorPaths>
       </configuration>
   </plugin>
   ```

2. **Executado `mvn clean compile -DskipTests -U`**
   
   Resultado:
   ```
   [INFO] Compiling 95 source files with javac [debug parameters release 21] to target\classes
   [INFO] BUILD SUCCESS
   ```

### Resultado

✅ Lombok funcionando corretamente  
✅ Getters/setters sendo gerados automaticamente  
✅ 95 arquivos compilados sem erros  
✅ Apenas warnings de null-safety (não afetam compilação)

### Lição Aprendida

Sempre especifique a versão do Lombok no `annotationProcessorPaths` do `maven-compiler-plugin`, mesmo que o Lombok já esteja nas dependências. O Maven precisa dessa informação para processar as anotações corretamente durante a compilação.

**Status**: ✅ RESOLVIDO COMPLETAMENTE
