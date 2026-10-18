# Proposta de notificações para o Ceris Mobile

Análise de código e contratos em 17/10/2026. Referências locais: mobile em `c47ba02` e Notify em `09794a4`. Este documento é uma proposta; não implementa nem confirma uma integração em execução.

## O que já existe

O Notify gera avisos a partir de candidatos do Core, salva o histórico no MongoDB e, quando habilitado, envia mensagens de dados pelo Firebase para instalações ativas do perfil. O agendamento padrão é diário, às 11h em America/Sao_Paulo.

| Tipo | Regra atual do servidor |
| --- | --- |
| PRODUCT_EXPIRING | 3 dias, 1 dia e no dia do vencimento. |
| UPCOMING_EVENT | 7 dias, 3 dias, 1 dia e no dia do evento. |
| NEARBY_STORE | Sugestão por endereço cadastrado, com rotação e intervalo mínimo. |
| NEW_NEARBY_STORE | Prioriza novidades descobertas no mesmo fluxo de sugestões de lojas. |

Lojas usam raio padrão de 5 km e intervalo mínimo padrão de 72 horas entre sugestões do perfil. O mobile não precisa de GPS para essas regras: o servidor utiliza o endereço cadastrado. As regras de geração continuam no Notify.

O mobile possui Retrofit, autenticação com JWT, repositórios e ViewModels, mas não possui Firebase Messaging, service de recebimento, cadastro da instalação, canais, solicitação da permissão Android ou central de notificações. Os detalhes de produto e local são telas sem carregamento por ID; não foi encontrada uma tela de detalhe de evento.

## Contrato a implementar

Todas as rotas abaixo usam o JWT do Auth em `Authorization: Bearer ...`. Não exigem API_KEY ou CERISIA_API_KEY. O servidor resolve o perfil pelo subject do JWT.

| Operação | Contrato |
| --- | --- |
| Cadastrar/sincronizar | PUT /api/push/installations, corpo {"installationId":"FID"}; retorna 200 com {"id":"ID_INTERNO","status":"ACTIVE"}. |
| Desvincular | DELETE /api/push/installations/{idInterno}; retorna 204 sem corpo. |
| Histórico | GET /api/notifications?page=0&size=20; retorna {"content":[...],"hasNext":true/false}. |
| Leitura | PATCH /api/notifications/{notificationId}/read; retorna 204 sem corpo e preserva a primeira leitura. |

`installationId` enviado no PUT é o Firebase Installation ID registrado no FCM. O `id` recebido é o ID do documento no Notify, usado no DELETE. Não são intercambiáveis. O PUT é idempotente para o mesmo FID e pode transferir seu vínculo para outro perfil autenticado.

Payload de dados do push, com valores string:

```json
{
  "notificationId": "670c00000000000000000001",
  "profileId": "12",
  "type": "PRODUCT_EXPIRING",
  "sourceId": "45",
  "title": "Produto próximo do vencimento",
  "message": "O produto Leite vence amanhã."
}
```

O histórico retorna `id`, `type`, `sourceId`, `title`, `message`, `createdAt`, `readAt` e `read`. Não retorna `profileId`, pois a lista já é filtrada pelo JWT. O push não inclui `createdAt` nem `readAt` e não substitui esse histórico.

## Arquitetura proposta

Preservar Kotlin, XML, Retrofit e a divisão atual de pacotes. Para a nova tela, expor estado observável do ViewModel com coleta vinculada ao lifecycle, evitando listeners que retenham uma Activity.

```text
Core -> Notify -> MongoDB (histórico)
                -> Firebase (mensagem de dados)
                     -> CerisMessagingService
                          -> validar sessão e perfil
                          -> deduplicar
                          -> NotificationPresenter
                               -> toque -> central de notificações

Central -> NotificationRepository -> NotifyAPI -> histórico/leitura
Login / abertura autenticada / onRegistered -> PushSyncCoordinator
                                          -> PushRegistrationWorker -> PUT
Logout -> bloquear recebimento local / cancelar sync -> DELETE -> limpar sessão
```

Componentes sugeridos; os nomes abaixo representam arquivos novos, não existentes:

| Local | Responsabilidade |
| --- | --- |
| CerisApplication.kt | Composição com applicationContext, renovação de sessão e canais; funciona sem abrir a splash. |
| api/NotifyAPI.kt | Quatro endpoints do contrato. |
| model/dto/*Notification*.kt | DTOs separados para histórico, paginação, cadastro e payload de push. |
| repository/NotificationRepository.kt | Histórico e marcação de leitura. |
| repository/PushInstallationRepository.kt | Cadastro e desvinculação autenticados. |
| local/PushInstallationStore.kt | FID confirmado, ID interno, perfil vinculado, estado de sincronização e deduplicação limitada. |
| notification/CerisMessagingService.kt | onRegistered, onMessageReceived e onDeletedMessages. |
| notification/NotificationChannels.kt | Canais estáveis para produtos, eventos e lojas. |
| notification/NotificationPresenter.kt | Notificação local, identidade estável e PendingIntent. |
| notification/PushSyncCoordinator.kt | Orquestra sessão, permissão, FID e trabalho único. |
| worker/PushRegistrationWorker.kt | Sincronização recuperável com restrição de rede e backoff. |
| notification/NotificationRouter.kt | Validação de destino e encaminhamento após autenticação. |
| viewmodel/NotificationsViewModel.kt | Estado da central e paginação. |
| view/NotificationsActivity.kt e view/adapter/NotificationAdapter.kt | Lista XML, leitura, estados vazio/erro/carregamento. |

### Firebase e configuração

- Registrar o applicationId atual `com.example.ceris` no mesmo projeto Firebase usado pelo Notify. Se o applicationId for mudar, decidir antes desta configuração.
- Adicionar o google-services.json Android no módulo app, plugin com.google.gms.google-services e Firebase BoM fixada que resolva firebase-messaging 25.1.0 ou superior, versão que introduziu o fluxo por FID. Usar o módulo principal firebase-messaging.
- Habilitar no application do Manifest a metadata `firebase_messaging_installation_id_enabled=true`.
- Registrar o service com exported=false e intent action com.google.firebase.MESSAGING_EVENT.
- Obter o FID confirmado pelo callback `onRegistered(installationId)`. `register()` permite solicitar nova confirmação, inclusive quando já registrado. Não usar getToken/onNewToken ou apenas um FID obtido de Installations como prova de registro bem-sucedido no FCM.
- O callback pode ocorrer sem login: salvar o FID confirmado e aguardar sessão elegível para enviar o PUT.
- Adicionar NOTIFY_API_BASE_URL ao modelo local, aos campos obrigatórios do BuildConfig e ao ApiBaseUrl. Configuração ausente desabilita a integração com mensagem apropriada; não deve fazer chamadas a localhost.
- Atualizar CI para fornecer configuração Android Firebase e URL de Notify de ambiente de teste, ou uma variante explicitamente sem push. O build não pode aparentar suporte a push com configuração incompleta.
- A conta de serviço Firebase, sua chave privada, FIREBASE_CREDENTIALS_BASE64, JWT_SECRET_BASE64 e NOTIFY_CORE_TOKEN permanecem exclusivamente no servidor. O script atual transforma quase todo local.properties em BuildConfig: não colocar esses segredos nele.

### Inicialização e identidade da sessão

Hoje a renovação HTTP é configurada em SplashScreen.onCreate. Um service ou worker pode iniciar o processo sem passar por essa Activity; mover a composição necessária para CerisApplication.

Ampliar a representação de sessão para guardar profileId e um identificador local de geração da sessão, além dos tokens. O Notify interpreta `sub` como o profileId inteiro positivo. Como os DTOs atuais do Auth não fornecem profileId explícito, ele pode ser extraído do payload do JWT recebido do Auth para comparação local. Isso não substitui validação de assinatura ou autorização no servidor; a chave de assinatura nunca vai para o app. Em sessões antigas, derivar esse dado do token existente ou exigir nova autenticação se não for possível.

Centralizar o estabelecimento da sessão nos fluxos de senha, Google e verificação OTP, aproveitando o ponto comum atual AuthRepository.saveTokens. A sincronização push é disparada após persistir a sessão e não bloqueia o sucesso do login. Renovação de access token mantém a mesma geração local; login novo, logout e troca de perfil a alteram.

Corrigir o RenovadorDeSessao: serializar a renovação concorrente, reaproveitar tokens já renovados e não descartar sessão por 429 ou 5xx. Respostas tardias de refresh não podem restaurar tokens de uma sessão encerrada ou sobrescrever outra conta.

### Permissão, canais e preferências

O projeto usa minSdk 24 e targetSdk 36. Criar canais no Android 8/API 26+, por exemplo ceris_produtos, ceris_eventos e ceris_lojas. No Android 7/API 24-25, usar os recursos compatíveis de notificação sem depender de NotificationChannel.

Solicitar POST_NOTIFICATIONS em runtime no Android 13/API 33+, após contextualizar os alertas na área autenticada. Recusa não bloqueia login, listas ou histórico. Verificar também bloqueio geral do app e do canal antes de publicar, e oferecer acesso às configurações pelo perfil.

Proposta para o MVP: associar a instalação ao Notify quando há sessão ativa, registro FCM confirmado e pelo menos um canal relevante permitido. Reavaliar ao retornar ao app; se tudo estiver bloqueado, desvincular quando possível, mantendo a central disponível. O backend atual não tem preferências por tipo: o controle por canal é local, e o servidor pode continuar enviando mensagens de tipos silenciados.

### Sincronização recuperável

Reconciliar instalação após login, onRegistered, abertura/retorno autenticado ao app e concessão de permissão. Evitar PUT por frame ou por simples redraw. Trabalho único e verificação de sessão/FID impedem duplicações locais; um PUT periódico durante retomadas atualiza last_seen_at e reativa registros INVALID.

O Worker recebe identificadores da geração/perfil esperados e lê credenciais atuais na execução. Não persistir JWT em WorkRequest. Antes de enviar e ao aplicar a resposta, conferir geração, perfil e FID; cancelar trabalho e Calls ao encerrar a sessão. Guardar o ID interno retornado junto ao FID e perfil correspondentes.

Falhas de rede, 429 e 5xx usam repetição limitada com backoff e nova reconciliação na próxima abertura; respeitar Retry-After quando aplicável. Um 401 permite a renovação coordenada, sem loop infinito. Erros permanentes de corpo/configuração não devem ser repetidos indefinidamente.

Quando o FID mudar, cadastrar o novo e tentar desvincular o registro antigo ainda pertencente ao perfil, sem apagar o vínculo local novo. Não renovar o FID manualmente em todo login.

### Recebimento e exibição

Em onMessageReceived, validar presença/formato dos campos, existência de sessão ativa, igualdade de profileId, permissão e canal. Repetir a checagem de sessão imediatamente antes da publicação. Sem sessão ou com outro perfil, descartar sem exibir título/corpo.

Manter um registro persistente e limitado dos notificationIds exibidos por perfil, com expiração/limite, e usar tag de notificação baseada no perfil + notificationId. Isso evita repetição e alerta duplicado após reinício. Não marcar como exibido quando a permissão impediu a publicação. Usar ícone monocromático de status e NotificationCompat.

O servidor envia mensagens de dados, não payload notification: o mobile é responsável pela exibição tanto em foreground quanto em background. Publicar prontamente com o conteúdo já recebido; não bloquear essa publicação aguardando GET/refresh HTTP ou busca de imagens. Processamento mais longo vai para WorkManager. onDeletedMessages solicita reconciliação do histórico, sem recriar avisos antigos como push.

Receber ou dispensar o aviso não significa lê-lo. Marcar leitura quando o usuário efetivamente abrir a notificação ou seu item na central, confirmando a atualização no servidor; falhas mantêm tentativa pendente vinculada ao mesmo perfil.

### Toque e central de notificações

Usar PendingIntent explícito, imutável e distinto por notificationId, com validação da sessão no destino. O caminho de abertura deve funcionar com processo vivo ou morto e preservar a navegação de volta ao app. Se precisar de login, guardar apenas o destino não sensível e só retomá-lo se o perfil autenticado corresponder; nunca abrir conteúdo de outro perfil.

MVP: todos os tipos abrem NotificationsActivity, com histórico atualizado, paginação por hasNext, destaque de lido/não lido, refresh e tratamento de erro. Adicionar entrada visível no estoque ou perfil, aproveitando o ícone ic_menu_bell existente.

Não existe GET /api/notifications/{id}. Portanto, um aviso antigo pode não aparecer na primeira página; a implementação deve continuar a paginação quando necessário ou abrir a central sem prometer posicionamento exato imediato. O PATCH por ID já permite registrar leitura sem inventar endpoint de detalhe.

O backend também não fornece total de não lidas. Não exibir um contador global calculado só pela primeira página. Deixar o badge numérico fora do MVP, ou criar um endpoint próprio depois.

Uma etapa posterior pode usar sourceId para abrir produto, evento ou loja após integrar suas telas ao Core. O Notify não fornece os detalhes completos dessas entidades. Tipos desconhecidos devem continuar aparecendo na central como aviso genérico.

### Logout e troca de conta

O logout atual limpa SessionManager diretamente. Substituir por uma coordenação que primeiro marque a sessão local como encerrando, bloqueie publicações e callbacks tardios, cancele sincronizações/Calls e remova notificações/cache do perfil anterior. Em seguida, tentar DELETE pelo ID interno com credenciais do perfil que está saindo, antes de descartá-las. Permitir uma janela curta para revogação/renovação se necessária; rede indisponível não pode prender o usuário no app.

Se o DELETE falhar, concluir o logout local e registrar que a revogação remota não foi confirmada. Não guardar JWT antigo em uma fila de trabalho. Enquanto a sessão estiver ausente, mensagens em trânsito são descartadas. No próximo login, o PUT reconcilia o FID com o perfil atual.

Cancelamento de chamada local não garante que uma requisição já recebida pelo servidor seja desfeita. O contrato atual não tem versão de vínculo: corridas remotas, especialmente com reconexão à mesma conta, não são eliminadas por um identificador apenas local. Se a exigência for isolamento estrito entre sessões, acrescentar versão de vínculo compartilhada no Notify e no payload. O fluxo atual permite um MVP com filtro por perfil, cancelamento, serialização e reconciliação, com esse limite explícito.

## Dependências e limites do backend

- NOTIFY_PUSH_ENABLED deve estar true no ambiente usado, com credenciais válidas e projeto Firebase correspondente ao Android; o padrão do código é false. A presença de código não comprova a configuração do deploy.
- MongoDB e índices de notifications/push_installations devem estar preparados. O fluxo de lojas também depende das collections próprias e suporte a transações.
- Core deve fornecer candidatos reais e o scheduler precisa executar. Não há endpoint público de envio manual no contrato analisado.
- O push tem prioridade Android alta e TTL de uma hora. Aceitação pelo Firebase não comprova recebimento, exibição ou leitura.
- O Notify envia somente avisos recém-criados; não tem outbox persistente ou recuperação de falha entre salvar e enviar. Habilitar push ou cadastrar aparelho depois não reenvia o histórico. O mobile recupera histórico pela API, mas não consegue corrigir essa perda de entrega do servidor.
- GET por ID, contador global de não lidas, preferências remotas, confirmação de entrega/exibição e versão de vínculo não existem hoje. Não devem aparecer como capacidades prontas da primeira entrega.

## Tasks propostas

| ID | Task | Dependências | Critério de aceite |
| --- | --- | --- | --- |
| N01 | Preparar Firebase Android, BoM compatível com FID, plugin, google-services.json, NOTIFY_API_BASE_URL e CI. | Acesso ao projeto Firebase e URL Notify. | Build de teste resolve onRegistered/register, flag FID habilitada e configuração sem credenciais privadas. |
| N02 | Criar CerisApplication e centralizar inicialização de sessão, clientes e canais. | N01. | Service e Worker iniciam com processo frio, sem depender de SplashScreen ou referência a Activity. |
| N03 | Ampliar estado de sessão com perfil/geração e corrigir renovação concorrente/erros temporários. | Nenhuma; pode começar antes de N01. | Dois 401 concorrentes não descartam sessão válida; refresh tardio não restaura sessão encerrada; 500 não apaga tokens. |
| N04 | Implementar NotifyAPI, DTOs e repositórios de histórico/instalação. | N01. | PUT/GET interpretam os contratos reais; DELETE/PATCH aceitam 204; autenticação usa só Bearer. |
| N05 | Implementar registro FCM por onRegistered e PushInstallationStore. | N01, N02. | FID confirmado é persistido mesmo antes do login; ID interno e FID permanecem distintos; troca de FID não reutiliza vínculo errado. |
| N06 | Implementar permissão, canais e acesso às configurações. | N01, N02. | API 33+ solicita em contexto; recusa mantém app utilizável; APIs 24-25 e canais bloqueados são tratados. |
| N07 | Implementar PushSyncCoordinator/Worker e ligação com todos os logins e retomadas. | N03, N04, N05, N06. | Cadastro eventual funciona após falha de rede e não bloqueia login; trabalho antigo não aplica resposta na sessão nova. |
| N08 | Implementar recebimento, filtro por perfil, deduplicação e publicação local. | N02, N03, N05, N06. | Payload elegível gera um aviso; duplicado não alerta duas vezes; sem sessão/perfil diferente/canal bloqueado não publica. |
| N09 | Implementar central XML/ViewModel/adapter, paginação e leitura. | N03, N04. | Lista funciona mesmo sem permissão de push; hasNext controla paginação; PATCH confirma leitura; erro não vira lista vazia. |
| N10 | Implementar clique seguro, rota de abertura e preservação do destino no login. | N08, N09. | Clique abre a central com processo vivo ou morto; outra conta não acessa o destino antigo; navegação de volta funciona. |
| N11 | Integrar logout e troca de conta ao cancelamento, DELETE e limpeza local. | N03, N04, N07, N08. | DELETE tenta usar a sessão antiga antes da limpeza; logout offline termina; avisos antigos somem e mensagens em trânsito são filtradas. |
| N12 | Validar contratos, concorrência e fluxo real Android -> Notify -> FCM -> Android. | N01 a N11. | Matriz abaixo executada e evidências de exibição/leitura coletadas; log de aceitação sozinho não encerra o teste. |
| N13 | Integrar navegação de sourceId para detalhes de produto/evento/loja. | N10 e respectivos endpoints/telas do Core. | Destino carrega dados atuais por ID e trata recurso removido ou sem acesso. Etapa posterior ao MVP. |
| N14 | Evoluir confiabilidade no Notify: outbox/retry, versão de vínculo e, se necessários, GET por ID, contagem e preferências. | Requisitos de produto e desenho dos contratos. | Falha após persistir pode ser recuperada sem perda/duplicação indevida; requisição de vínculo antiga não sobrescreve sessão nova. Evolução de backend. |

Ordem sugerida: começar por N01/N03, seguir N02/N04/N05/N06, integrar N07/N08/N09, concluir N10/N11 e executar N12. N13 e N14 são evoluções separadas. Não é necessário implementar AlarmManager, polling periódico de regras, GPS ou a nova navegação de detalhes para entregar o primeiro push e a central.

## Validação da entrega

Automatizar parsing dos DTOs, filtro de perfil, validação de payload, deduplicação persistente, códigos HTTP/204, renovação concorrente, troca de geração, cancelamento de workers e aplicação de respostas tardias. Usar relógio controlado onde houver expiração e não confundir estado local de publicação com confirmação real do Android.

Testar em Android 7/API 24-25, Android 8+/canais, Android 13+/permissão e Android 16/API 36. Usar aparelho físico ou emulador com Google Play services, internet e ambiente Firebase correspondente.

Matriz funcional: foreground, background, processo encerrado pelo sistema, forçar parada pelas configurações, Doze/restrições, reinício, permissão recusada/revogação de canal, duplicação do payload, dois aparelhos do mesmo perfil, logout online/offline, troca A->B, reentrada na mesma conta, reinstall/clear data, mudança do FID, falha/retorno de rede e access token expirado. Tratar forçar parada como condição de entrega limitada; não prometer comportamento equivalente a background normal.

Para o teste ponta a ponta, preparar um candidato real do perfil no Core e executar o scheduler em frequência curta apenas no ambiente de teste. Verificar aviso persistido, instalação ativa, envio aceito, notificação visível no Android e leitura 204. Repetir a geração não reenvia o aviso já existente. Restaurar a configuração de teste ao final.

## Evidências e documentação

Arquivos analisados no Notify: README.md, docs/push-installations.md, docs/push-delivery.md, controller/PushInstallationController.java, controller/NotificationController.java, DTOs, service/PushInstallationService.java, service/NotificationService.java, service/PushDispatchService.java, infra/push/FirebasePushProvider.java, infra/repository/PushInstallationRepository.java, infra/security/SecurityConfiguration.java, util/CurrentProfileResolver.java, regras e application.yml. Caminhos Java relativos a src/main/java/com/example/nexusnotify.

Pontos de integração no mobile: app/build.gradle.kts, gradle/libs.versions.toml, AndroidManifest.xml, api/ApiBaseUrl.kt, api/RetrofitClient.kt, api/RenovadorDeSessao.kt, local/SessionManager.kt, repository/AuthRepository.kt, view/SplashScreen.kt, view/MainActivity.kt, view/fragment/PerfilFragment.kt e DTOs de sessão. Caminhos Kotlin relativos a app/src/main/java/com/example/ceris.

Referências oficiais consultadas:

- [Firebase Android: configuração](https://firebase.google.com/docs/android/setup).
- [Firebase Android: registro por FID e permissão](https://firebase.google.com/docs/cloud-messaging/android/get-started).
- [Release notes: Messaging 25.1.0 introduz FID](https://firebase.google.com/support/release-notes/android#messaging_v25-1-0).
- [FirebaseMessagingService: onRegistered e onMessageReceived](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessagingService).
- [FirebaseMessaging: register e unregister](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessaging).
- [Mensagens de dados e processamento Android](https://firebase.google.com/docs/cloud-messaging/android/receive-messages).
- [Firebase Admin Java: Message.Builder.setFid](https://firebase.google.com/docs/reference/admin/java/reference/com/google/firebase/messaging/Message.Builder).
- [Android: canais de notificação](https://developer.android.com/develop/ui/views/notifications/channels).
