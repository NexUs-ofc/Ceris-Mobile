# Navegacao das notificacoes

O app abre o estoque para avisos de produto e o detalhe para avisos de loja ou evento.
O detalhe de produto aguarda a tela correta do projeto. Nao ha central nem historico de notificacoes.
A permissao de notificacoes usa somente o dialogo do Android (Android 13+).

## Contrato

O payload do Notify continua igual: `notificationId`, `profileId`, `type`, `sourceId`, `title` e `message`.
O mobile cria o endereco abaixo para o PendingIntent. Nenhum token, titulo ou mensagem entra no link.

```text
ceris://notification/{profileId}/{notificationId}?type={type}&sourceId={sourceId}
```

| Tipo | Destino | Origem no Core | Voltar |
| --- | --- | --- | --- |
| PRODUCT_EXPIRING | Estoque (provisorio) | Sem consulta de detalhe; sourceId permanece no link | — |
| NEARBY_STORE / NEW_NEARBY_STORE | DetalheLocalActivity | GET /api/stores/{sourceId} | Locais |
| UPCOMING_EVENT | DetalheEventoActivity | GET /api/events/{sourceId} | Inicio |

Links malformados e de outros perfis sao recusados. O Core autoriza a consulta com o JWT atual.
As respostas ficam vinculadas a geracao da sessao; trocar de conta ou sair invalida a consulta.
Sem sessao, a MainActivity guarda somente o destino por ate 15 minutos e abre o login.
Depois do login (senha ou Google), consome e valida esse destino uma unica vez.
O logout explicito apaga o destino pendente.

Conteudo removido/sem acesso mostra uma mensagem e o botao de retorno da tabela.
Falha temporaria/rede oferece nova tentativa. Um 401 definitivo pede login novamente.
Os detalhes de loja e evento exibem dados atuais do Core; nao mostram uma copia do texto do push.
Quando o detalhe correto de produto estiver disponivel, ligar o destino PRODUCT na MainActivity
a essa tela, usando sourceId como id do item de estoque (nao foodId) e mantendo a validacao de sessao.
Links antigos sem tipo/sourceId nao permitem identificar o registro e recebem mensagem de link invalido.

## Validacao no aparelho

1. Instalar o app limpo e entrar: aparece apenas o pedido de permissao do Android. Recusar nao impede o uso.
2. Receber cada um dos quatro tipos, com registros existentes, e tocar com o app fechado, em segundo plano e aberto.
3. Para produto, conferir que abre o estoque sem exibir o detalhe generico. Para loja/evento, conferir
   o registro, seus dados e o retorno pelo botao da tela e pelo gesto Voltar do Android.
4. Tocar em outro push com um detalhe aberto: abrir o novo destino e retornar a aba correspondente.
5. Girar a tela no detalhe e voltar: o detalhe nao deve reabrir sozinho.
6. Abrir um link sem sessao; entrar com senha e repetir com Google: retomar o destino uma vez.
7. Entrar com outra conta: recusar o destino sem carregar dados do perfil anterior.
8. Remover a loja/evento antes do toque: mostrar conteudo indisponivel e permitir voltar.
9. Desligar a rede antes do toque em aviso de loja/evento: mostrar erro e permitir tentar novamente.
10. Abrir link com ID/tipo invalido: recusar sem fechar o app.

Exemplo para teste via Android Debug Bridge, substituindo pelos IDs da conta e do registro:

```sh
adb shell 'am start -a android.intent.action.VIEW -d "ceris://notification/12/notice-1?type=PRODUCT_EXPIRING&sourceId=45" com.example.ceris'
```
