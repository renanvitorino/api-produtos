# Controle de estoque da API de Produtos

## 1. Objetivo

Este documento descreve a evolução da API de Produtos para controlar o estoque de cada produto. A solução mantém a arquitetura existente, baseada em Javalin, camadas de Controller, Service e Repository, JDBC e banco H2.

O controle permite informar o saldo inicial no cadastro, consultar produtos com saldo baixo e registrar entradas e saídas. As operações são persistidas no banco para que o saldo continue disponível após reiniciar a aplicação.

## 2. Funcionamento da solução

O atributo `quantidadeEstoque` foi adicionado ao modelo `Produto` e é armazenado na coluna `quantidade_estoque` da tabela `produtos`.

As responsabilidades seguem as camadas existentes:

- **Controller (`ProdutoController`)**: recebe as requisições HTTP, interpreta parâmetros e corpos JSON, encaminha as operações ao Service e define os códigos de resposta.
- **Service (`ProdutoService`)**: aplica as regras de negócio, como validar quantidades positivas, impedir limites negativos e tratar produto inexistente.
- **Repository (`ProdutoRepository` e `ProdutoRepositoryJDBC`)**: acessa os dados. O repositório JDBC usa `PreparedStatement` para comandos e consultas parametrizados.
- **Database (`Database`)**: inicializa o esquema e atualiza bancos existentes.

### Persistência e compatibilidade

O `CREATE TABLE IF NOT EXISTS` define a coluna de estoque para bancos novos. Para bancos já criados, a inicialização executa `ALTER TABLE produtos ADD COLUMN IF NOT EXISTS quantidade_estoque INT NOT NULL DEFAULT 0`. Assim, produtos existentes passam a ter saldo zero sem remover seus demais dados.

### Segurança contra saldo negativo

A saída não consulta o saldo para depois gravar um novo valor. Ela executa uma única atualização condicional:

```sql
UPDATE produtos
SET quantidade_estoque = quantidade_estoque - ?
WHERE id = ? AND quantidade_estoque >= ?
```

O banco só atualiza o registro quando há saldo suficiente. Se nenhuma linha for atualizada, o Service verifica se o produto existe; se existir, rejeita a operação por estoque insuficiente. Essa abordagem evita saldo negativo inclusive quando saídas concorrentes são solicitadas.

## 3. Endpoints

| Método e rota | Descrição | Sucesso |
|---|---|---|
| `POST /produtos` | Cadastra produto; `quantidadeEstoque` é opcional e assume `0` quando omitida. | `201 Created` |
| `POST /produtos/{id}/estoque/entrada` | Soma a quantidade informada ao estoque. | `200 OK` |
| `POST /produtos/{id}/estoque/saida` | Subtrai a quantidade informada se houver saldo suficiente. | `200 OK` |
| `GET /produtos/estoque-baixo?limite=5` | Lista produtos cujo estoque é menor ou igual ao limite. | `200 OK` |

O JSON das operações de entrada e saída tem o formato:

```json
{"quantidade": 10}
```

As respostas de erro seguem o formato `{"erro":"mensagem clara"}`. Entradas inválidas, quantidade nula ou menor/igual a zero, saldo insuficiente, limite inválido, ID não numérico e JSON inválido devem retornar `400 Bad Request`. Um ID numérico sem produto correspondente retorna `404 Not Found`. Detalhes internos de exceções são registrados no log do servidor, não enviados ao cliente.

## 4. Plano de testes de TI

Os casos abaixo verificam as regras principais por meio da API. Eles são procedimentos de teste documentados; os resultados devem ser preenchidos após execução no ambiente de teste.

| ID | Tipo | Cenário | Resultado esperado |
|---|---|---|---|
| CT-01 | Integração | Cadastrar produto com `quantidadeEstoque: 10`. | `201`; resposta inclui `quantidadeEstoque: 10`. |
| CT-02 | Integração | Cadastrar produto sem informar `quantidadeEstoque`. | `201`; saldo retornado igual a `0`. |
| CT-03 | Integração | Fazer entrada de `5` unidades em produto existente. | `200`; saldo aumenta em cinco unidades. |
| CT-04 | Integração | Fazer saída de `3` unidades com saldo suficiente. | `200`; saldo diminui em três unidades. |
| CT-05 | Integração | Fazer saída maior que o saldo disponível. | `400`; corpo contém `erro`; saldo permanece inalterado. |
| CT-06 | Integração | Enviar quantidade `0`, negativa ou nula em entrada/saída. | `400`; operação não altera o saldo. |
| CT-07 | Integração | Fazer entrada/saída usando ID numérico inexistente. | `404`; corpo contém `erro`. |
| CT-08 | Integração | Usar ID não numérico em uma rota de produto. | `400`; corpo contém `erro`. |
| CT-09 | Integração | Enviar JSON malformado em uma operação com corpo. | `400`; corpo contém somente a mensagem de erro da API, sem stack trace. |
| CT-10 | Integração | Consultar estoque baixo com limite `5`, incluindo saldos `0`, `5` e `6`. | `200`; inclui os produtos com saldo `0` e `5`, exclui o de saldo `6`. |
| CT-11 | Integração | Consultar estoque baixo com limite ausente, não inteiro ou negativo. | `400`; corpo contém `erro`. |
| CT-12 | Persistência | Registrar saldo, reiniciar a API e consultar o produto novamente. | `200`; saldo permanece igual ao valor gravado antes do reinício. |
| CT-13 | Migração | Inicializar a aplicação com banco existente sem a coluna de estoque. | A coluna é criada e produtos anteriores ficam com saldo `0`; dados antigos continuam acessíveis. |
| CT-14 | Concorrência | Com saldo conhecido, enviar saídas concorrentes cuja soma supere o saldo. | Apenas saídas cobertas pelo saldo são aceitas; nenhuma resposta bem-sucedida deixa saldo negativo. |

### Critérios de aceite

- Os códigos HTTP e os formatos de resposta correspondem aos definidos neste documento.
- Operações rejeitadas não alteram o saldo.
- A consulta de estoque baixo inclui o limite (`estoque <= limite`).
- O saldo persiste após reinicialização e produtos antigos continuam disponíveis depois da migração.
- Em nenhum cenário o saldo fica negativo ou a resposta HTTP revela stack trace/detalhes internos.

## 5. Roteiro manual com `curl.exe` no PowerShell

Inicie a API e execute os comandos abaixo na mesma janela do PowerShell. O primeiro comando cria um produto de teste com saldo inicial `10` e guarda o ID retornado:

```powershell
$produto = curl.exe -sS -X POST "http://localhost:7000/produtos" -H "Content-Type: application/json" -d '{"nome":"Produto Teste Estoque","preco":10,"quantidadeEstoque":10}' | ConvertFrom-Json
$id = $produto.id
$produto
```

### Entrada válida

Adiciona cinco unidades. Espera-se `200 OK` e saldo `15`.

```powershell
curl.exe -i -X POST "http://localhost:7000/produtos/$id/estoque/entrada" -H "Content-Type: application/json" -d '{"quantidade":5}'
```

### Saída válida

Retira três unidades. Espera-se `200 OK` e saldo `12`.

```powershell
curl.exe -i -X POST "http://localhost:7000/produtos/$id/estoque/saida" -H "Content-Type: application/json" -d '{"quantidade":3}'
```

### Saída maior que o saldo

Espera-se `400 Bad Request`; o saldo deve continuar em `12`.

```powershell
curl.exe -i -X POST "http://localhost:7000/produtos/$id/estoque/saida" -H "Content-Type: application/json" -d '{"quantidade":999}'
```

### Quantidade zero

Espera-se `400 Bad Request`; o saldo não deve mudar.

```powershell
curl.exe -i -X POST "http://localhost:7000/produtos/$id/estoque/entrada" -H "Content-Type: application/json" -d '{"quantidade":0}'
```

### Produto inexistente

Espera-se `404 Not Found`.

```powershell
curl.exe -i -X POST "http://localhost:7000/produtos/999999999/estoque/entrada" -H "Content-Type: application/json" -d '{"quantidade":1}'
```

### Consulta de estoque baixo

Espera-se `200 OK` com produtos cujo saldo seja menor ou igual a `5`.

```powershell
curl.exe -i "http://localhost:7000/produtos/estoque-baixo?limite=5"
```

### Persistência após reiniciar

Confirme primeiro o saldo, encerre e inicie a API novamente sem apagar a base H2. Depois, consulte o mesmo ID: o saldo esperado neste roteiro é `12`.

```powershell
curl.exe -i "http://localhost:7000/produtos/$id"
```

## 6. Registro dos resultados

Preencha esta tabela durante a execução dos testes no ambiente definido para o trabalho:

| Testes | Resultado (aprovado/reprovado) | Evidência / observações |
|---|---|---|
| CT-01 a CT-04 | Pendente | |
| CT-05 a CT-09 | Pendente | |
| CT-10 a CT-11 | Pendente | |
| CT-12 a CT-13 | Pendente | |
| CT-14 | Pendente | |
