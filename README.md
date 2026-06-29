# Sumativa Cloud Native

Microservicio Spring Boot para el caso **Sistema de Gestion de Pedidos y Generacion de Guias de Despacho**.

Cumple el flujo solicitado:

- Crear guia de despacho y guardarla temporalmente en EFS.
- Subir la guia a AWS S3 con estructura `YYYYMM/transportista/guiaID.pdf`.
- Descargar desde S3 con validacion de credencial del transportista.
- Actualizar, eliminar y consultar guias por transportista y fecha.
- Construir imagen Docker, publicarla en Docker Hub y desplegarla en EC2 con GitHub Actions.

## Stack

- Java 21
- Spring Boot 3.4.6
- Maven
- H2 con archivo local para historial
- AWS SDK S3
- Docker
- GitHub Actions

## Variables de entorno

| Variable | Uso |
| --- | --- |
| `APP_STORAGE_EFS_PATH` | Ruta donde la app escribe las guias temporales. En Docker debe ser `/app/efs`. |
| `APP_STORAGE_S3_BUCKET` | Bucket S3 donde se suben las guias. |
| `AWS_REGION` | Region AWS del bucket. |
| `AWS_ACCESS_KEY_ID` | Credencial AWS. |
| `AWS_SECRET_ACCESS_KEY` | Credencial AWS. |
| `AWS_SESSION_TOKEN` | Token temporal de AWS Academy, si aplica. |
| `APP_DATA_PATH` | Ruta para guardar la base H2 persistente. |

## Ejecutar local

```bash
mvn spring-boot:run
```

Para probar solo creacion local en EFS sin S3:

```bash
APP_STORAGE_EFS_PATH=./efs mvn spring-boot:run
```

Para usar S3 real:

```bash
APP_STORAGE_EFS_PATH=./efs \
APP_STORAGE_S3_BUCKET=tu-bucket \
AWS_REGION=us-east-1 \
AWS_ACCESS_KEY_ID=... \
AWS_SECRET_ACCESS_KEY=... \
AWS_SESSION_TOKEN=... \
mvn spring-boot:run
```

## Endpoints

Base URL: `http://localhost:8080/api/guias`

### Crear guia

```http
POST /api/guias
Content-Type: application/json
```

```json
{
  "transportista": "transportistaX",
  "credencialDescarga": "clave123",
  "fecha": "202606",
  "contenido": "Pedido 1001, destino Santiago, bultos 3"
}
```

La app crea un archivo temporal bajo `APP_STORAGE_EFS_PATH/YYYYMM/transportista/`.

### Subir guia generada a S3

```http
POST /api/guias/{idGuia}/s3
```

Genera el objeto `YYYYMM/transportista/guia{idGuia}.pdf`.

### Descargar guia con permiso

```http
POST /api/guias/{idGuia}/descarga
Content-Type: application/json
```

```json
{
  "credencialDescarga": "clave123"
}
```

Si la credencial no coincide con la del transportista, responde `403`.

### Actualizar guia

```http
PUT /api/guias/{idGuia}
Content-Type: application/json
```

```json
{
  "transportista": "transportistaX",
  "credencialDescarga": "clave123",
  "fecha": "202606",
  "contenido": "Pedido 1001 actualizado, destino Valparaiso, bultos 4"
}
```

Sobrescribe el archivo temporal y actualiza el objeto S3.

### Eliminar guia

```http
DELETE /api/guias/{idGuia}
```

Elimina el objeto S3 y marca la guia como `ELIMINADA` en el historial.

### Consultar por transportista y fecha

```http
GET /api/guias?transportista=transportistaX&fecha=202606
```

Devuelve historial filtrado por transportista y periodo `YYYYMM`.

## Docker

```bash
docker build -t sumativa-cloud-native .
docker run -p 8080:8080 \
  -v /mnt/efs:/app/efs \
  -v "$PWD/data":/app/data \
  -e APP_STORAGE_EFS_PATH=/app/efs \
  -e APP_DATA_PATH=/app/data \
  -e APP_STORAGE_S3_BUCKET=tu-bucket \
  -e AWS_REGION=us-east-1 \
  -e AWS_ACCESS_KEY_ID=... \
  -e AWS_SECRET_ACCESS_KEY=... \
  -e AWS_SESSION_TOKEN=... \
  sumativa-cloud-native
```

## GitHub Secrets requeridos

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`
- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY`
- `AWS_S3_BUCKET`
- `AWS_REGION`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN`

## Evidencia EFS

En EC2, montar EFS en `/mnt/efs` antes del despliegue:

```bash
sudo mkdir -p /mnt/efs
sudo mount -t nfs4 -o nfsvers=4.1 fs-xxxxxxxx.efs.us-east-1.amazonaws.com:/ /mnt/efs
df -h | grep efs
```

El workflow ejecuta el contenedor con:

```bash
-v /mnt/efs:/app/efs
```

Despues de crear una guia, validar:

```bash
ls -R /mnt/efs
```
# Sumantiva_Cloud_Native
