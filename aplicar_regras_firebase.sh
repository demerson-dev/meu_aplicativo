#!/bin/bash
# Script para aplicar as regras do Firebase automaticamente
# Execute este script após fazer login no Firebase CLI

echo "==================================="
echo "   Aplicando Regras do Firebase"
echo "==================================="
echo ""

# Verifica se o Firebase CLI está instalado
if ! command -v firebase &> /dev/null
then
    echo "❌ Firebase CLI não está instalado."
    echo "Instale com: npm install -g firebase-tools"
    exit 1
fi

echo "✅ Firebase CLI encontrado"
echo ""

# Faz login (se necessário)
echo "Verificando autenticação..."
firebase login:list

echo ""
echo "Aplicando regras do Firebase Realtime Database..."
firebase deploy --only database

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Regras aplicadas com sucesso!"
    echo ""
    echo "Próximos passos:"
    echo "1. Faça rebuild do app no Android Studio"
    echo "2. Faça login no app"
    echo "3. Tente criar um treino"
    echo ""
else
    echo ""
    echo "❌ Erro ao aplicar regras. Verifique:"
    echo "1. Se você está logado no Firebase CLI"
    echo "2. Se o projeto está configurado corretamente"
    echo ""
    echo "Para configurar o projeto, execute:"
    echo "firebase init database"
fi

