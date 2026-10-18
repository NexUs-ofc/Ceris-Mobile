# Base de notificacoes: primeira PR

Esta entrega corresponde a N01, N02, N03 e N04 da proposta. Mantem Kotlin, layouts XML, Retrofit e os callbacks dos repositorios atuais. Nenhum commit e necessario para revisar a implementacao pelo staged.

## Firebase e ambiente

O arquivo Android fornecido foi copiado para `app/google-services.json`. Antes da copia, foram comparados o pacote `com.example.ceris`, o projeto `ceris-bfbfe`, o FIREBASE_PROJECT_ID e o project_id da credencial local do Notify. Os projetos coincidem. Essa verificacao nao consulta a configuracao do deploy nem comprova entrega de mensagens.

O catalogo Gradle usa Firebase BoM 35.0.1, o modulo principal firebase-messaging e o plugin Google Services 4.5.0. O Manifest habilita o modo FID. O registro automatico do Messaging permanece desabilitado ate a proxima PR implementar o service, o cadastro e a politica de permissao.

A URL fornecida foi configurada como `https://ceris-notify.discloud.dev/` no local.properties, no modelo e como reserva explicita do CI. O arquivo local.properties continua ignorado pelo Git. Sem URL configurada, os novos repositorios retornam erro de configuracao antes de tentar acessar localhost.

O build gera BuildConfig apenas para uma lista explicita de propriedades Android. Credenciais privadas do Notify nao devem ser colocadas no projeto mobile. O JSON Android fornecido e diferente da conta de servico usada no backend.

## Inicializacao

`CerisApplication` configura a renovacao HTTP usando applicationContext e cria canais estaveis para produtos, eventos e lojas no Android 8+. A configuracao saiu da SplashScreen para atender tambem processos iniciados por services e workers.

Criar canais nao solicita permissao nem publica avisos. A permissao em runtime e o recebimento entram na segunda PR.

## Sessao e concorrencia

`SessionManager.startSession` e chamado pelo ponto comum ja existente, AuthRepository.saveTokens. Assim senha, Google e OTP passam a criar uma nova geracao local da sessao sem alterar os contratos dos ViewModels.

`JwtProfile` extrai o subject inteiro positivo apenas para identificar localmente o perfil. A assinatura e a autorizacao continuam nos servidores; nenhuma chave JWT foi incorporada ao aplicativo.

`SessionSnapshot` associa geracao, perfil e tokens. Sessoes existentes recebem os novos metadados quando consultadas. Uma renovacao preserva a geracao; login novo e limpeza da sessao invalidam o estado anterior. Os tokens nao aparecem no toString do snapshot.

`SessionRequestInterceptor` associa requisicoes autenticadas ao snapshot. Nos endpoints novos, o snapshot tambem acompanha a criacao do Call via @Tag, sem ser enviado como campo HTTP. Uma chamada enfileirada de outra sessao e recusada; uma chamada da mesma geracao pode usar o access token ja renovado.

O Retrofit autenticado usa uma Call.Factory que captura o snapshot antes da fila do OkHttp. Isso atende tambem ShoppingListAPI e ChatAPI, que nao recebem @Tag nos metodos. Sem essa captura, uma chamada dessas APIs com token antigo podia falhar se outra chamada renovasse enquanto ela aguardava na fila. A fabrica preserva tags explicitas, corpo, URL, metodo e X-API-Key; somente o Bearer e atualizado pelo interceptor durante uma renovacao da mesma sessao. Um token que ja nao corresponde a sessao na criacao da chamada continua sendo recusado, pois nao ha evidencia de que pertence a mesma sessao.

`RenovadorDeSessao` serializa tentativas concorrentes e reaproveita a renovacao concluida por outra chamada. A aplicacao dos tokens e condicional ao snapshot original, permitindo logout durante a chamada sem restaurar a sessao ao receber sua resposta. HTTP 429 e 5xx preservam os tokens; HTTP 401 na renovacao descarta somente a sessao correspondente. O desafio ApiKey continua sem consumir refresh.

O log HTTP compartilhado usa BASIC em debug para evitar corpos contendo senha, tokens ou FID. O arquivo de preferencias da sessao foi excluido do backup e da transferencia entre aparelhos, para nao restaurar uma identidade local de sessao em outra instalacao.

## API e repositorios

`NotifyAPI` implementa o contrato atual, somente com Bearer do Auth:

| Metodo | Rota | Resposta |
| --- | --- | --- |
| PUT | /api/push/installations | PushInstallationResponse: id interno e status. |
| DELETE | /api/push/installations/{id} | 204, sem corpo. |
| GET | /api/notifications?page=0&size=20 | SliceResponse: content e hasNext. |
| PATCH | /api/notifications/{id}/read | 204, sem corpo. |

O FID do PUT e o ID interno usado no DELETE sao distintos. Os DTOs preservam datas como texto ISO-8601 e tipos como String, permitindo tratar tipos novos em uma versao futura da interface.

`NotificationRepository` oferece listar/marcarComoLida. `PushInstallationRepository` oferece registrar/desvincular. Ambos recebem API e SessionManager por construtor, seguem callbacks de sucesso/erro HTTP/falha e retornam Call para permitir cancelamento na proxima etapa.

`NotifyCallback` valida a geracao antes de entregar uma resposta. Uma sessao encerrada ou substituida nao recebe sucesso da chamada antiga. Resposta de historico/cadastro sem corpo resulta em falha de protocolo; nao se transforma em sucesso vazio. DELETE e PATCH aceitam sucesso sem corpo.

## CI e testes

O workflow preserva as quatro verificacoes existentes numa matriz: testes, Android Lint, APK e ktlint. A configuracao Firebase e conferida por pacote/projeto, e as propriedades locais sao escritas a partir de variaveis de ambiente sem interpolar segredos em codigo shell.

Os novos testes cobrem migracao de sessao, renovacao concorrente, logout durante refresh, troca de perfil, invalidacao de callbacks, chamada enfileirada, formato dos endpoints e do corpo, respostas 204, tipos desconhecidos, hasNext e configuracao Android Firebase. Nao fazem envios reais ao FCM nem chamadas autenticadas ao Notify de producao.

Verificacoes normais em um ambiente Android configurado:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug ktlintCheck
```

O ambiente desta sessao restringe os sockets locais usados pelos workers Java do Gradle. Para verificacao local, foram usados argumentos temporarios da JVM, compilacao direta do BuildConfig gerado e execucao direta de ferramentas quando necessario. Os scripts desse contorno ficam somente em build/verification e nao integram o staged.

Resultado local: codigo principal e testes compilados, APK debug gerado, Android Lint aprovado e 44 testes JUnit aprovados (32 novos e 12 existentes). O ktlint 1.0.1 configurado pelo projeto passou nos arquivos novos e nos arquivos formatados desta entrega (SessionManager, RenovadorDeSessao e scripts Gradle). A verificacao geral de ktlint ainda aponta problemas de estilo em codigo preexistente, como settings.gradle.kts, AuthAPI e os testes de exemplo; por isso nao se declara o pipeline inteiro aprovado.

Revisao de isolamento: APK debug recompilado, Android Lint aprovado novamente e seis cenarios adicionais aprovados para APIs existentes, logout, preservacao de tickets e conversa durante refresh. O CI preserva as tarefas amplas `test` e `lint` do workflow anterior, em vez de reduzir a cobertura apenas ao debug.

## Proximas PRs

Esta base ainda nao registra o aparelho no Notify nem exibe push. A segunda PR implementa FirebaseMessagingService/onRegistered, sincronizacao, permissao, publicacao e desvinculacao no logout. A terceira implementa central, leitura e abertura dos avisos.

## Efeitos sobre outras funcionalidades

| Area | Efeito e motivo |
| --- | --- |
| Listas e chat | Chamadas enfileiradas passam a identificar a sessao antes da fila; podem acompanhar a renovacao do token sem trocar corpo ou API key. Logout ou novo login invalida chamadas antigas ainda nao enviadas. |
| Chamadas ja em andamento | O interceptor nao cancela operacoes ja enviadas. Os callbacks existentes de listas/chat nao receberam filtro de geracao nesta PR; uma resposta atrasada ainda pode chegar a uma tela antiga. Isso e uma limitacao preexistente, distinta do filtro implementado nos repositorios novos do Notify. |
| Login com senha, Google e OTP | Continuam usando AuthRepository.saveTokens. Cada login cria uma geracao nova e remove CHAT_SESSION_ID para nao reaproveitar a conversa de outra sessao. Tickets de cadastro, Google e recuperacao sao preservados. A conclusao de uma conversa antiga no servidor nao e garantida por essa limpeza local. |
| Renovacao | Renova uma vez para chamadas concorrentes; 429/5xx e falha de rede mantem a sessao. Um 401 de refresh invalida apenas a sessao correspondente. A conversa atual e preservada durante renovacao normal. |
| Restauracao em outro aparelho | ceris_preferences.xml deixa de ser restaurado, exigindo novo login. Como esse arquivo tambem guarda tickets de cadastro/recuperacao e o ID da conversa, esses dados tambem ficam fora da restauracao. Os extras locais das listas usam outro arquivo. |
| Diagnostico HTTP | BASIC substitui BODY no debug: deixa de mostrar corpos das requisicoes e respostas nos logs compartilhados. Nao muda o conteudo HTTP. |
| Inicializacao | A renovacao e os canais passam a ser configurados no Application, inclusive sem Splash. Criacao dos canais nao solicita permissao, exibe aviso nem chama o Notify. |
| Build e CI | Todas as constantes BuildConfig atualmente referenciadas pelo codigo continuam na lista permitida. Novas propriedades Android precisam entrar nela. O CI preserva `test`, `lint`, `assembleDebug` e `ktlintCheck`; o job de estilo ainda tem falhas preexistentes e o workflow nao foi executado no GitHub nesta sessao. |

## Incertezas e decisoes pendentes

Esta lista separa verificacao de ambiente de decisoes de produto. Nao significa que todos esses pontos bloqueiam a base desta PR. O dominio informado e o projeto Firebase local ja foram conferidos; nao precisam ser escolhidos novamente.

| Ponto | O que ainda nao foi confirmado | Como resolver |
| --- | --- | --- |
| Deploy do Notify | Projeto Firebase, credencial/permissoes, push habilitado, scheduler, fuso e indices Mongo efetivamente usados no Discloud. O arquivo local nao comprova esses valores. | Conferir o ambiente implantado e seus logs sem expor segredos. |
| Auth implantado | JWTs reais de todos os fluxos com subject numerico positivo, rotacao de refresh e codigos de revogacao. A base segue o contrato do Notify e o comportamento de refresh ja esperado no mobile. | Testar senha, Google e OTP com tokens reais no ambiente de teste. |
| Login Google | Assinaturas debug/release e clientes OAuth autorizados no console; compatibilidade em execucao apos adicionar o SDK. O login continua usando GOOGLE_WEB_CLIENT_ID, nao um valor substituido pelo JSON. | Validar login em aparelho com as duas assinaturas e conferir os clientes no console. |
| CI e release | Secrets existentes no GitHub, resultado do workflow remoto e comportamento do APK release. A verificacao local usa debug. | Executar CI e validar a configuracao de release antes da distribuicao. |
| Entrega Android | Registro FID real, mensagens em primeiro/segundo plano, morte do processo, Doze, fabricantes, reinstalacao e restricoes do aparelho. | Implementar a PR de recebimento e executar a matriz em dispositivo com Google Play services. |
| Pedido de permissao | Em qual tela/momento pedir permissao e qual orientacao mostrar apos recusa ou bloqueio do canal. | Definir o fluxo de produto antes da PR de recebimento. |
| Cadastro sem permissao | Se o aparelho deve continuar vinculado no Notify quando a permissao e negada/revogada. O cadastro na API nao comprova permissao Android. | Definir politica de vinculo e sincronizacao. |
| Logout offline | A proposta e concluir o logout local mesmo se o DELETE falhar; faltam aprovacao dessa politica e desenho de eventuais novas tentativas sem vincular a conta errada. | Definir a politica antes de integrar logout push. |
| Frequencia de sincronizacao | Quando repetir o PUT para atualizar last_seen_at e reativar um cadastro INVALID, e como tratar falhas de rede. | Definir e testar os gatilhos na PR de registro. |
| Mensagem da mesma conta apos novo login | O payload atual nao tem uma versao de vinculo. A geracao local protege chamadas HTTP, mas nao identifica a sessao que originou um push do mesmo perfil. | Aceitar esse limite ou evoluir o contrato no Notify. |
| Clique e leitura | Se tocar no push ja marca como lido ou apenas abrir o item na central; efeito sobre outros aparelhos e contador. A API ja compartilha o estado por perfil. | Definir a regra de experiencia e atualizar o estado via API. |
| Destino de sourceId | Tela definitiva para cada tipo, consulta do recurso atual e tratamento de recurso removido/sem acesso. Produto usa ID do item; evento e loja usam seus IDs de origem. | Integrar com os endpoints e telas reais, evitando assumir que ID do item e ID do catalogo. |
| Clique sem sessao | Se guardar o destino ate concluir o login e como descarta-lo quando a conta autenticada for outra. | Definir o fluxo de navegacao e isolamento por perfil. |
| Privacidade e apresentacao | Conteudo na tela bloqueada, som, agrupamento e importancia por categoria. A base criou tres canais DEFAULT como proposta inicial. | Validar com produto antes de publicar avisos reais. |
| Duplicacao local | Periodo de retencao dos IDs publicados e sua limpeza na troca de conta/reinstalacao. | Definir persistencia e limpeza na PR de recebimento. |
| Preferencias e elegibilidade | Se o usuario pode desativar categorias pelo app e quais tipos de conta recebem cada aviso. O contrato consumido nao oferece preferencias individuais. | Definir regra de produto e eventual mudanca de API. |
| Garantia de entrega | O Notify atual nao recupera pushes perdidos apos salvar o historico; habilitar push depois nao envia avisos antigos. Nao esta confirmado se esse nivel de entrega atende o produto. | Aceitar historico como recuperacao ou planejar outbox/retry no backend. |
| Regras de geracao | Antecedencias, horario/fuso, frequencia de lojas e notificacao para varias instalacoes existem no servidor; a aprovacao de produto e os valores de producao nao foram confirmados aqui. | Validar configuracao e requisitos sem duplicar as regras no mobile. |
| Conversa entre logins | Esta PR limpa o ID local a cada login. Nao ha confirmacao de uma exigencia de retomar a mesma conversa apos autenticar novamente. | Confirmar a regra; eventual encerramento remoto precisa usar a sessao antiga. |

As verificacoes automatizadas nao equivalem a uma garantia de todos os fluxos em aparelho ou no deploy real. A matriz integrada e as decisoes acima continuam registradas para as proximas PRs.

Referencias oficiais: [Firebase Android](https://firebase.google.com/docs/android/setup) e [registro por FID](https://firebase.google.com/docs/cloud-messaging/android/get-started).
