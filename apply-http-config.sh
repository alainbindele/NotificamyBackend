#!/bin/bash
# Script per applicare la configurazione che permette HTTP

echo "🔧 Configurazione Apache per permettere HTTP e HTTPS"
echo "===================================================="

# 1. Backup configurazione attuale
echo "💾 Backup configurazione attuale..."
sudo cp /etc/apache2/sites-enabled/notificamy.conf /etc/apache2/sites-enabled/notificamy.conf.backup.$(date +%Y%m%d_%H%M%S)

# 2. Applica nuova configurazione
echo "📝 Applicazione nuova configurazione..."
sudo cp apache-http-allowed.conf /etc/apache2/sites-available/notificamy.conf

# 3. Test configurazione
echo "🧪 Test configurazione Apache..."
sudo apache2ctl configtest

if [ $? -eq 0 ]; then
    echo "✅ Configurazione valida"
    
    # 4. Riavvia Apache
    echo "🔄 Riavvio Apache..."
    sudo systemctl reload apache2
    
    # 5. Test funzionalità
    echo "🧪 Test funzionalità..."
    
    echo "Test backend diretto:"
    curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8080/api/v1/health
    
    echo "Test proxy Apache HTTP:"
    curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://notificamy.com/api/v1/health
    
    echo "Test proxy Apache HTTPS:"
    curl -s -o /dev/null -w "HTTPS Status: %{http_code}\n" https://notificamy.com/api/v1/health
    
    echo ""
    echo "✅ Configurazione applicata!"
    echo "📋 Ora il sito funziona sia con HTTP che HTTPS:"
    echo "   - http://notificamy.com"
    echo "   - https://notificamy.com"
    echo ""
    echo "🔧 Aggiorna il frontend per usare:"
    echo "   BASE_URL: 'http://notificamy.com' (senza porta 8080)"
    
else
    echo "❌ Errore nella configurazione"
    echo "Ripristino backup..."
    sudo cp /etc/apache2/sites-enabled/notificamy.conf.backup.* /etc/apache2/sites-available/notificamy.conf
    sudo systemctl reload apache2
fi