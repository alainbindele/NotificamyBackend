#!/bin/bash
# Script per risolvere il problema Apache 401

echo "🔧 Risoluzione problema Apache 401"
echo "=================================="

# 1. Backup configurazione attuale
echo "💾 Backup configurazione attuale..."
sudo cp /etc/apache2/sites-enabled/notificamy.conf /etc/apache2/sites-enabled/notificamy.conf.backup.$(date +%Y%m%d_%H%M%S)

# 2. Disabilita tutti i moduli di autenticazione
echo "🔒 Disabilitazione moduli autenticazione..."
sudo a2dismod auth_openidc 2>/dev/null || true
sudo a2dismod authn_core 2>/dev/null || true
sudo a2dismod authz_user 2>/dev/null || true

# 3. Applica configurazione minima
echo "📝 Applicazione configurazione minima..."
sudo cp apache-minimal.conf /etc/apache2/sites-available/notificamy.conf

# 4. Test configurazione
echo "🧪 Test configurazione..."
sudo apache2ctl configtest

if [ $? -eq 0 ]; then
    echo "✅ Configurazione valida"
    
    # 5. Riavvia Apache
    echo "🔄 Riavvio Apache..."
    sudo systemctl reload apache2
    
    # 6. Aspetta un momento
    sleep 2
    
    # 7. Test funzionalità
    echo "🧪 Test funzionalità..."
    
    echo "Test backend diretto:"
    curl -s -o /dev/null -w "Status: %{http_code}\n" http://localhost:8080/api/v1/health
    
    echo "Test proxy Apache:"
    curl -s -o /dev/null -w "Status: %{http_code}\n" https://notificamy.com/api/v1/health
    
    echo ""
    echo "✅ Se entrambi i test restituiscono 200, il problema è risolto!"
    echo "❌ Se il problema persiste, controlla i log:"
    echo "   sudo tail -f /var/log/apache2/notificamy_error.log"
    
else
    echo "❌ Errore nella configurazione"
    echo "Ripristino backup..."
    sudo cp /etc/apache2/sites-enabled/notificamy.conf.backup.* /etc/apache2/sites-available/notificamy.conf
    sudo systemctl reload apache2
fi