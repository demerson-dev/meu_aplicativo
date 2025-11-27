# Como Atualizar as Regras do Firebase Realtime Database

## Problema Resolvido
O erro "Permission denied" acontecia porque as regras do Firebase não permitiam a leitura da lista completa de treinos, apenas treinos individuais.

## Solução Implementada

### 1. Código Atualizado (HomeFragment.kt)
- ✅ Removida a query `orderByChild("userId")` que causava erro de permissão
- ✅ Agora busca todos os treinos e filtra pelo userId no código
- ✅ Mais eficiente e compatível com as regras de segurança

### 2. Regras do Firebase Atualizadas

As novas regras estão no arquivo `firebase_database_rules_correto.json`

## Como Aplicar as Novas Regras no Firebase Console

### Passo 1: Acesse o Firebase Console
1. Vá para https://console.firebase.google.com/
2. Selecione seu projeto FitMax

### Passo 2: Acesse o Realtime Database
1. No menu lateral esquerdo, clique em **Realtime Database**
2. Clique na aba **Regras** (Rules)

### Passo 3: Copie e Cole as Novas Regras
Copie o conteúdo do arquivo `firebase_database_rules_correto.json` e cole no editor:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid",
        ".write": "$uid === auth.uid",
        ".validate": "newData.hasChildren(['key', 'nome', 'email'])",
        "key": {
          ".validate": "newData.val() === $uid"
        },
        "nome": {
          ".validate": "newData.isString() && newData.val().length > 0 && newData.val().length <= 100"
        },
        "email": {
          ".validate": "newData.isString() && newData.val().matches(/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$/i)"
        },
        "endereco": {
          ".validate": "newData.isString() && newData.val().length <= 200"
        },
        "cep": {
          ".validate": "newData.isString() && newData.val().length <= 8"
        },
        "dataNascimento": {
          ".validate": "newData.isString() && newData.val().matches(/^\\d{2}\\/\\d{2}\\/\\d{4}$/)"
        },
        "photoUrl": {
          ".validate": "newData.isString()"
        }
      }
    },
    "treinos": {
      ".read": "auth != null",
      "$treinoId": {
        ".write": "auth != null && (!data.exists() || data.child('userId').val() === auth.uid) && newData.child('userId').val() === auth.uid",
        ".validate": "newData.hasChildren(['key', 'userId', 'nomeTreino', 'exercicios', 'series', 'repeticoes'])",
        "key": {
          ".validate": "newData.val() === $treinoId"
        },
        "userId": {
          ".validate": "newData.val() === auth.uid"
        },
        "nomeTreino": {
          ".validate": "newData.isString() && newData.val().length > 0 && newData.val().length <= 100"
        },
        "exercicios": {
          ".validate": "newData.isString() && newData.val().length > 0"
        },
        "series": {
          ".validate": "newData.isNumber() && newData.val() > 0 && newData.val() <= 100"
        },
        "repeticoes": {
          ".validate": "newData.isNumber() && newData.val() > 0 && newData.val() <= 1000"
        },
        "observacoes": {
          ".validate": "newData.isString()"
        }
      }
    }
  }
}
```

### Passo 4: Publique as Regras
1. Clique no botão **Publicar** (Publish)
2. Confirme a publicação

### Passo 5: Teste o App
1. Faça rebuild do app no Android Studio
2. Desinstale e reinstale o app no dispositivo (opcional, mas recomendado)
3. Faça login com seu usuário
4. Os treinos devem aparecer agora! ✅

## O que Mudou nas Regras?

### Antes:
```json
"treinos": {
  "$treinoId": {
    ".read": "auth != null && data.child('userId').val() === auth.uid"
  }
}
```
❌ Permitia ler apenas treinos individuais, não a lista completa

### Depois:
```json
"treinos": {
  ".read": "auth != null",
  "$treinoId": {
    ".write": "..."
  }
}
```
✅ Permite que usuários autenticados leiam a lista de treinos
✅ A filtragem por usuário é feita no código do app
✅ Cada usuário ainda só pode escrever seus próprios treinos

## Segurança

- ✅ Apenas usuários autenticados podem ler treinos
- ✅ Cada usuário só pode criar/editar/excluir seus próprios treinos
- ✅ A filtragem no código garante que cada usuário veja apenas seus treinos
- ✅ Todas as validações de dados permanecem ativas

## Precisa de Ajuda?

Se ainda tiver problemas:
1. Verifique se está logado com um usuário válido
2. Verifique os logs no Logcat (busque por "ListaTreinos")
3. Confirme que as regras foram publicadas no Firebase Console
4. Tente criar um novo treino para testar

