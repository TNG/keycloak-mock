@echo off
SET PATH=%PATH%;%CD%\bin\nodejs\node\
for /d %%i in (%CD%\bin\pnpm\pnpm-v*) do set PNPM_DIR=%%i
"%PNPM_DIR%\bin\pnpm.cmd" %*
