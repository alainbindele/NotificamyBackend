#!/bin/bash
# Script per diagnosticare e risolvere il problema Apache 401

echo "🔍 Diagnosi problema Apache 401 Unauthorized"
echo "============================================="

# 1. Verifica configurazione Apache attuale
echo "📋 Configurazione Apache attuale:"
sudo apache2ctl -S

echo ""
echo "🔧 Moduli Apache caricati:"
apache2ctl -M | grep -E "(auth|ssl|proxy|headers)"

echo ""
echo "🧪 Test diretto backend:"
curl -v http://localhost:8080/api/v1/health

echo ""
echo "🧪 Test proxy Apache senza autenticazione:"
curl -v -k https://notificamy.com/api/v1/health

echo ""
echo "📊 Processi Apache:"
ps aux | grep apache2

echo ""
echo "📋 Log Apache in tempo reale (ultimi 10 righe):"
echo "=== ERROR LOG ==="
sudo tail -10 /var/log/apache2/notificamy_error.log 2>/dev/null || echo "Nessun error log"

echo ""
echo "=== ACCESS LOG ==="
sudo tail -10 /var/log/apache2/notificamy_access.log 2>/dev/null || echo "Nessun access log"

echo ""
echo "🔧 SOLUZIONI POSSIBILI:"
echo "1. Rimuovi completamente autenticazione Apache"
echo "2. Verifica che non ci siano altri virtual host in conflitto"
echo "3. Controlla che non ci siano moduli auth attivi"
echo "4. Testa con configurazione minima"

echo ""
echo "🚀 Vuoi applicare la configurazione minima? (y/n)"
read -r response
if [[ "$response" =~ ^[Yy]$ ]]; then
    echo "Applicando configurazione minima..."
    sudo cp /etc/apache2/sites-enabled/notificamy.conf /etc/apache2/sites-enabled/notificamy.conf.backup
    echo "Backup creato in notificamy.conf.backup"
fi