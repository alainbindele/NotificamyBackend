#!/bin/bash
# Script per configurare Apache con HTTPS per NotifyMe

echo "🔧 Configurazione Apache per NotifyMe con HTTPS"

# 1. Abilita i moduli necessari
echo "📦 Abilitazione moduli Apache..."
sudo a2enmod ssl
sudo a2enmod headers
sudo a2enmod proxy
sudo a2enmod proxy_http
sudo a2enmod rewrite
sudo a2enmod expires

# 2. Copia la configurazione del sito
echo "📝 Configurazione del virtual host..."
sudo cp apache-notificamy.conf /etc/apache2/sites-available/notificamy.conf

# 3. Disabilita il sito default se presente
sudo a2dissite 000-default.conf 2>/dev/null || true
sudo a2dissite default-ssl.conf 2>/dev/null || true

# 4. Abilita il nuovo sito
sudo a2ensite notificamy.conf

# 5. Testa la configurazione
echo "🧪 Test configurazione Apache..."
sudo apache2ctl configtest

if [ $? -eq 0 ]; then
    echo "✅ Configurazione Apache valida"
    
    # 6. Riavvia Apache
    echo "🔄 Riavvio Apache..."
    sudo systemctl reload apache2
    
    echo "✅ Apache configurato con successo!"
    echo ""
    echo "📋 Prossimi passi:"
    echo "1. Assicurati che i certificati SSL siano in:"
    echo "   - /etc/ssl/certs/notificamy.com.crt"
    echo "   - /etc/ssl/private/notificamy.com.key"
    echo ""
    echo "2. Verifica che Spring Boot sia in ascolto su porta 8080:"
    echo "   curl http://localhost:8080/api/v1/health"
    echo ""
    echo "3. Testa il proxy HTTPS:"
    echo "   curl https://notificamy.com/api/v1/health"
    echo ""
    echo "4. Aggiorna il frontend per usare:"
    echo "   BASE_URL: 'https://notificamy.com' (senza porta 8080)"
    
else
    echo "❌ Errore nella configurazione Apache"
    echo "Controlla i log: sudo journalctl -u apache2 -f"
fi