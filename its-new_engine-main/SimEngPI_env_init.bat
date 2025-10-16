@echo off

set PY_VERSION=%~dp0py3.11.0
set PY_HOME=%~dp0pyenv\versions\py3.11.0
set PY_PIP=%~dp0pyenv\versions\py3.11.0\Scripts
set PY_LIBS=%~dp0pyenv\versions\py3.11.0\Lib\site-packages

set PYTHONHOME=%~dp0pyenv\versions\py3.11.0
set PYTHONIOENCODING=utf-8
set "PATH=%PY_HOME%;%PY_PIP%;%PYTHONHOME%;%PY_LIBS%;%PATH%"

echo Current Python Env: %PY_HOME%