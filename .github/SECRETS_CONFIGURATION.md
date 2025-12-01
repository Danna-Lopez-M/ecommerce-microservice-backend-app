# Configuración de Secrets para GitHub Actions

Este documento describe todos los secrets que deben configurarse en GitHub para que los pipelines funcionen correctamente.

## Secrets Requeridos

### Docker Hub
- **`DOCKER_USERNAME`**: Nombre de usuario de Docker Hub
- **`DOCKER_PASSWORD`**: Token o contraseña de Docker Hub

### SonarQube (Opcional pero recomendado)
- **`SONAR_TOKEN`**: Token de autenticación de SonarQube
- **`SONAR_HOST_URL`**: URL del servidor SonarQube (ej: `https://sonarcloud.io` o `https://sonarqube.example.com`)

### Notificaciones por Email (Opcional)
- **`SMTP_SERVER`**: Servidor SMTP (ej: `smtp.gmail.com`, `smtp.office365.com`)
- **`SMTP_PORT`**: Puerto SMTP (ej: `587` para TLS, `465` para SSL)
- **`SMTP_USERNAME`**: Usuario del servidor SMTP
- **`SMTP_PASSWORD`**: Contraseña del servidor SMTP
- **`SMTP_FROM`**: Dirección de email desde la cual se enviarán las notificaciones
- **`NOTIFICATION_EMAIL`**: Dirección de email que recibirá las notificaciones de fallos

## Cómo Configurar los Secrets

1. Ve a tu repositorio en GitHub
2. Navega a **Settings** > **Secrets and variables** > **Actions**
3. Haz clic en **New repository secret**
4. Agrega cada secret con su nombre y valor correspondiente

## Configuración de SonarQube

### Opción 1: SonarCloud (Recomendado para proyectos open source)
1. Ve a [SonarCloud.io](https://sonarcloud.io)
2. Inicia sesión con tu cuenta de GitHub
3. Crea un nuevo proyecto o conéctalo a tu repositorio
4. Copia el token generado y configúralo como `SONAR_TOKEN`
5. Configura `SONAR_HOST_URL` como `https://sonarcloud.io`

### Opción 2: SonarQube Self-Hosted
1. Configura tu instancia de SonarQube
2. Genera un token de usuario en SonarQube
3. Configura `SONAR_TOKEN` con el token generado
4. Configura `SONAR_HOST_URL` con la URL de tu instancia

## Configuración de Notificaciones por Email

### Gmail
```
SMTP_SERVER: smtp.gmail.com
SMTP_PORT: 587
SMTP_USERNAME: tu-email@gmail.com
SMTP_PASSWORD: tu-app-password (no tu contraseña normal, usa App Password)
SMTP_FROM: tu-email@gmail.com
NOTIFICATION_EMAIL: destinatario@example.com
```

**Nota**: Para Gmail, necesitas generar una "App Password" desde tu cuenta de Google.

### Office 365 / Outlook
```
SMTP_SERVER: smtp.office365.com
SMTP_PORT: 587
SMTP_USERNAME: tu-email@outlook.com
SMTP_PASSWORD: tu-contraseña
SMTP_FROM: tu-email@outlook.com
NOTIFICATION_EMAIL: destinatario@example.com
```

### Otros Proveedores SMTP
Consulta la documentación de tu proveedor de email para obtener los valores correctos de SMTP_SERVER y SMTP_PORT.

## Notas Importantes

- **SonarQube es opcional**: Si no configuras los secrets de SonarQube, el pipeline continuará pero el análisis de SonarQube fallará silenciosamente (usando `continue-on-error: true`)
- **Notificaciones por email son opcionales**: Si no configuras los secrets de SMTP, el job de notificación fallará silenciosamente sin afectar el pipeline
- **Docker Hub es requerido**: Los secrets de Docker Hub son necesarios para construir y publicar las imágenes

## Verificación

Después de configurar los secrets, puedes verificar que funcionan correctamente ejecutando un pipeline. Los jobs que requieren secrets mostrarán errores claros si falta algún secret requerido.

