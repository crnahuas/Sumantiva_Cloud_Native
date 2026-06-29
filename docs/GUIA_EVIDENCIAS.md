# Guia de evidencias para video y entrega

## 1. Repositorio y pipeline

Mostrar:

- Repositorio en GitHub.
- Workflow `.github/workflows/main.yml`.
- Ejecucion del pipeline con pasos de test, build, push a Docker Hub y deploy en EC2.
- Imagen publicada en Docker Hub.

## 2. EFS funcional

En EC2:

```bash
df -h | grep efs
docker inspect sumativa-cloud-native | grep /mnt/efs -n
ls -R /mnt/efs
```

Luego crear una guia desde Postman y volver a mostrar que aparece bajo:

```text
/mnt/efs/YYYYMM/transportista/
```

## 3. Flujo Postman sugerido

Importar:

- `docs/postman/Sumativa_Cloud_Native_Semana6_API_Gateway.postman_collection.json`
- `docs/postman/Sumativa_Cloud_Native_Semana6_API_Gateway.postman_environment.json`

El environment ya queda apuntando a:

```text
https://bab3x5yn3m.execute-api.us-east-1.amazonaws.com
```

Cuando API Gateway este listo, reemplazar `baseUrl` por el Invoke URL del stage, por ejemplo:

```text
https://bab3x5yn3m.execute-api.us-east-1.amazonaws.com
```

La coleccion usa `Authorization: Bearer {{accessToken}}`. Pegar en `accessToken` un token emitido por Azure AD B2C antes de probar contra API Gateway.

Ejecutar las solicitudes en orden. La coleccion guarda automaticamente `idGuia` y `s3Key`.

### Crear guia

`POST http://EC2_PUBLIC_IP:8080/api/guias`

```json
{
  "transportista": "transportistaX",
  "credencialDescarga": "clave123",
  "fecha": "202606",
  "contenido": "Pedido 1001, destino Santiago, bultos 3"
}
```

Guardar el `idGuia` de la respuesta.

### Subir a S3

`POST http://EC2_PUBLIC_IP:8080/api/guias/1/s3`

Mostrar en AWS S3 el objeto:

```text
202606/transportistaX/guia1.pdf
```

### Descargar con control de permisos

`POST http://EC2_PUBLIC_IP:8080/api/guias/1/descarga`

```json
{
  "credencialDescarga": "clave123"
}
```

Mostrar que descarga con la credencial correcta. Luego probar una credencial incorrecta:

```json
{
  "credencialDescarga": "incorrecta"
}
```

Debe responder `403`.

### Actualizar

`PUT http://EC2_PUBLIC_IP:8080/api/guias/1`

```json
{
  "transportista": "transportistaX",
  "credencialDescarga": "clave123",
  "fecha": "202606",
  "contenido": "Pedido 1001 actualizado, destino Valparaiso, bultos 4"
}
```

Mostrar que S3 conserva la misma ruta y queda actualizado.

### Consultar historial

`GET http://EC2_PUBLIC_IP:8080/api/guias?transportista=transportistaX&fecha=202606`

### Eliminar

`DELETE http://EC2_PUBLIC_IP:8080/api/guias/1`

Mostrar que el objeto ya no esta en S3 y que la respuesta deja estado `ELIMINADA`.

## 4. Apunte Blue/Green

Blue/Green es una tecnica de despliegue continuo donde se mantienen dos ambientes equivalentes:

- Blue: version actual estable.
- Green: nueva version candidata.

Pasos:

1. Pre-release: desplegar la nueva version en Green y validar salud, endpoints y logs.
2. Release: cambiar el trafico desde Blue hacia Green.
3. Post-release: monitorear errores y conservar Blue por un periodo corto para rollback.

Ventajas:

- Reduce tiempo de caida.
- Permite rollback rapido.
- Facilita validar una version nueva antes de exponerla a usuarios.

En esta sumativa el despliegue principal queda en EC2 con Docker, pero se documenta la tecnica como aprendizaje de continuidad operacional.
