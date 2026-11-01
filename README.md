# Ceris para Android

O Ceris ajuda a organizar os alimentos da casa: acompanha o estoque e as validades, monta listas de compras, encontra receitas e mercados e oferece assistência pelo chat. O app também permite incluir produtos lendo códigos de barras ou notas fiscais.

## Funcionalidades

- **Estoque:** cadastro, edição e descarte de produtos, busca, filtros e acompanhamento de validade.
- **Leitura de produtos:** leitura de códigos EAN/UPC no aparelho e consulta à Open Food Facts antes de oferecer o cadastro.
- **Notas fiscais:** captura ou seleção de uma imagem, leitura assistida por IA e revisão dos produtos e quantidades antes de adicionar ao estoque.
- **Listas:** criação de listas de compras e eventos, progresso dos itens, quantidades, valores estimados e data da lista.
- **Receitas:** consulta de receitas e modo de preparo passo a passo.
- **Locais:** busca de mercados e gerenciamento de endereços salvos.
- **Assistente:** conversa sobre estoque, compras e receitas, com sugestões de respostas contextuais quando a API as fornece.
- **Conta:** cadastro, login por e-mail, verificação, recuperação de senha e login com Google.
- **Notificações:** sincronização de notificações push pelo Firebase Cloud Messaging.

## Tecnologias

- Kotlin, Android Views e layouts XML.
- AndroidX, Material Components e ConstraintLayout.
- Organização por telas, `ViewModel`, repositórios e interfaces de API.
- Retrofit e OkHttp para comunicação HTTP.
- CameraX para preview e captura; ML Kit para leitura local de códigos de barras e detecção de texto.
- Credential Manager para autenticação com Google.
- Firebase Cloud Messaging para notificações.
- JUnit para testes unitários e KtLint para estilo Kotlin.

## Requisitos

- Android Studio com Android SDK Platform 36.
- JDK 17 ou compatível com a versão do Gradle do projeto.
- Android 7.0 (API 24) ou superior para executar o app.
- Acesso às configurações dos serviços usados pelo ambiente: APIs Ceris, Open Food Facts, Google Sign-In e Firebase.

O workflow de CI usa JDK 17 e executa testes, lint, compilação de debug e KtLint em pull requests para `main` e em atualizações de `main`.

## Configuração local

1. Clone o repositório e abra a pasta no Android Studio.
2. Crie `local.properties` a partir de `local.properties.example`:

   **Windows PowerShell**

   ```powershell
   Copy-Item local.properties.example local.properties
   ```

   **macOS ou Linux**

   ```bash
   cp local.properties.example local.properties
   ```

3. Configure os valores locais em `local.properties`:

   | Propriedade | Uso |
   | --- | --- |
   | `sdk.dir` | Caminho do Android SDK instalado na máquina. |
   | `AUTH_API_BASE_URL` | Endereço base do serviço de autenticação. |
   | `CORE_API_BASE_URL` | Endereço base do serviço principal do Ceris. |
   | `IA_API_BASE_URL` | Endereço base do serviço de IA usado na leitura de notas e no assistente. |
   | `NOTIFY_API_BASE_URL` | Endereço base do serviço de notificações. |
   | `VIA_CEP_URL` | Serviço de consulta de CEP. |
   | `API_KEY` | Chave exigida pelos serviços configurados. |
   | `CERISIA_API_KEY` | Chave usada pelo serviço Ceris IA. |
   | `GOOGLE_WEB_CLIENT_ID` | ID de cliente OAuth do tipo Web para Credential Manager. |
   | `ACCOUNT_TYPE` | Tipo de conta esperado pela instalação, por exemplo `HOUSEHOLD`. |

   Use valores fornecidos para o ambiente correto. `local.properties` é local e ignorado pelo Git; não coloque chaves ou tokens no código, no README ou em commits.

4. Configure o Firebase para o aplicativo `com.example.ceris` e disponibilize o `google-services.json` apropriado em `app/` quando necessário. Para o login Google, cadastre no projeto OAuth as assinaturas SHA-1 usadas para os builds de debug e release, junto ao ID de cliente Web correspondente.
5. Sincronize o projeto com o Gradle no Android Studio.

Sem configurações privadas, o build usa valores sentinela para que o app possa ser compilado. Login e chamadas a serviços que dependem dessas configurações não funcionarão até que os valores locais estejam corretos.

## Compilar e verificar

Na raiz do repositório:

```bash
./gradlew test lint assembleDebug ktlintCheck
```

No Windows PowerShell:

```powershell
.\gradlew.bat test lint assembleDebug ktlintCheck
```

O APK de debug é gerado em `app/build/outputs/apk/debug/app-debug.apk`. Com um dispositivo ou emulador autorizado conectado, instale-o com:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

O Android Studio também pode compilar, instalar e executar a configuração `app` diretamente.

## Estrutura do projeto

```text
app/src/main/java/com/example/ceris/
├── api/          Interfaces HTTP, clientes e configurações de serviços
├── local/        Sessão e dados locais do aparelho
├── model/        Modelos de domínio e DTOs
├── notification/ Notificações push e links recebidos
├── repository/   Acesso a dados, integrações e regras de consulta
├── util/         Utilitários compartilhados
├── view/         Activities, fragments, diálogos e adapters
└── viewmodel/    Estado e coordenação da interface

app/src/main/res/
├── layout/                 Layouts padrão
├── layout-land/            Composições específicas para paisagem
├── layout-sw600dp-land/    Composições de paisagem para tablets
└── values*/                Cores, textos, estilos e dimensões por configuração
```

Os layouts de retrato permanecem como recursos padrão. O Android escolhe variantes de paisagem e de tablets pelos qualificadores de recursos, sem exigir caminhos de navegação diferentes.

## CI

O workflow [Android CI](.github/workflows/android-ci.yml) roda quatro verificações independentes:

- testes unitários (`test`);
- análise Android Lint (`lint`);
- build do APK de debug (`assembleDebug`);
- verificação de estilo Kotlin (`ktlintCheck`).

As chaves de produção devem ser cadastradas como secrets do GitHub Actions. O workflow cria um `local.properties` temporário durante a execução e não depende do arquivo local do desenvolvedor.
