#!/bin/bash
# Script per debug della configurazione Apache

echo "🔍 Debug configurazione Apache per NotifyMe"
echo "============================================"

# 1. Verifica che Apache sia attivo
echo "📊 Status Apache:"
sudo systemctl status apache2 --no-pager -l

echo ""
echo "🔧 Moduli Apache abilitati:"
apache2ctl -M | grep -E "(ssl|proxy|headers|rewrite)"

echo ""
echo "🌐 Virtual hosts attivi:"
sudo apache2ctl -S

echo ""
echo "🧪 Test configurazione:"
sudo apache2ctl configtest

echo ""
echo "🔍 Test connessioni:"

# Test backend diretto
echo "📡 Test backend diretto (HTTP):"
curl -s -o /dev/null -w "Status: %{http_code}, Time: %{time_total}s\n" \
  http://localhost:8080/api/v1/health || echo "❌ Backend non raggiungibile"

# Test proxy Apache
echo "📡 Test proxy Apache (HTTPS):"
curl -s -o /dev/null -w "Status: %{http_code}, Time: %{time_total}s\n" \
  https://notificamy.com/api/v1/health || echo "❌ Proxy Apache non funziona"

echo ""
echo "📋 Log recenti Apache:"
echo "Errori:"
sudo tail -5 /var/log/apache2/notificamy_error.log 2>/dev/null || echo "Nessun log errori"

echo ""
echo "Accessi:"
sudo tail -5 /var/log/apache2/notificamy_access.log 2>/dev/null || echo "Nessun log accessi"

echo ""
echo "🔧 Comandi utili per debug:"
echo "- Logs in tempo reale: sudo tail -f /var/log/apache2/notificamy_error.log"
echo "- Restart Apache: sudo systemctl restart apache2"
echo "- Test backend: curl http://localhost:8080/api/v1/health"
echo "- Test frontend: curl https://notificamy.com/api/v1/health"
