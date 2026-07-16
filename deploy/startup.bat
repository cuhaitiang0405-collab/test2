@echo off
echo === MDT System Startup (Windows) ===
echo Step 1: Start Docker services...
docker compose up -d postgres redis rabbitmq minio
echo Waiting for Postgres...
timeout /t 5 /nobreak >nul
echo Step 2: Start microservices (run in separate terminals)...
echo   mdt-auth:    java -jar ..\backend\mdt-auth\target\mdt-auth-1.0.0.jar
echo   mdt-integration: java -jar ..\backend\mdt-integration\target\mdt-integration-1.0.0.jar
echo   mdt-image:   java -jar ..\backend\mdt-image\target\mdt-image-1.0.0.jar
echo   mdt-workflow: java -jar ..\backend\mdt-workflow\target\mdt-workflow-1.0.0.jar
echo   mdt-collab:  java -jar ..\backend\mdt-collab\target\mdt-collab-1.0.0.jar
echo   mdt-ext:     java -jar ..\backend\mdt-ext\target\mdt-ext-1.0.0.jar
echo Step 3: Start API Gateway...
echo   cd ..\backend\mdt-gateway ^&^& mvn spring-boot:run
echo Step 4: Start Frontend...
echo   cd ..\frontend ^&^& npm run dev
echo === Done. Gateway on http://localhost:8080, Frontend on http://localhost:5173 ===
