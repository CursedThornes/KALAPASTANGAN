@echo off
setlocal EnableExtensions EnableDelayedExpansion
title KALAPASTANGAN
pushd "%~dp0"

rem double click mo to, laro agad
rem kung wala syang makitang java, magtatanong muna bago ka iwan sa ere
rem may --check kung gusto mo lang malaman anong java at jar ang nahanap

rem hanapin yung jar
set "JAR="
for %%f in ("%~dp0lwjgl3\build\libs\KALAPASTANGAN-*.jar") do set "JAR=%%~ff"
if not defined JAR for %%f in ("%~dp0*.jar") do set "JAR=%%~ff"
if not defined JAR goto :nojar

rem hanapin yung java
set "JAVAW="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVAW=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVAW for /f "delims=" %%i in ('where javaw 2^>nul') do if not defined JAVAW set "JAVAW=%%i"
if not defined JAVAW for /f "delims=" %%i in ('where java 2^>nul') do if not defined JAVAW set "JAVAW=%%i"
if not defined JAVAW goto :nojava

rem javaw walang sinasabi sa console, kaya java.exe ang tinatanong natin
set "JAVAEXE=%JAVAW:javaw.exe=java.exe%"
set "VERFILE=%TEMP%\kalapastangan_java_version.txt"
set "JAVAVER="
"%JAVAEXE%" -version 2>"%VERFILE%"
for /f "tokens=3" %%v in ('findstr /i "version" "%VERFILE%"') do if not defined JAVAVER set "JAVAVER=%%~v"
if exist "%VERFILE%" del "%VERFILE%" >nul 2>&1
set "MAJOR="
for /f "tokens=1 delims=._-" %%a in ("%JAVAVER%") do set "MAJOR=%%a"
if "%MAJOR%"=="1" for /f "tokens=2 delims=._-" %%a in ("%JAVAVER%") do set "MAJOR=%%a"
if not defined MAJOR goto :oldjava
if %MAJOR% LSS 8 goto :oldjava

if /i "%~1"=="--check" goto :check

start "" "%JAVAW%" -jar "%JAR%"
goto :done

:check
echo Java:  "%JAVAW%"  (version %JAVAVER%)
echo Jar:   "%JAR%"
echo Check passed: the game would start now.
goto :done

:nojar
if /i "%~1"=="--check" goto :checknojar
echo.
echo   The game file (KALAPASTANGAN-*.jar) was not found.
echo   Put this launcher next to the jar, or build it first with:
echo       gradlew.bat lwjgl3:jar
echo.
pause
goto :done

:checknojar
echo Jar:   NOT FOUND
goto :done

:nojava
if /i "%~1"=="--check" goto :checknojava
powershell -NoProfile -ExecutionPolicy Bypass -Command "$m='KALAPASTANGAN needs Java to run.' + [Environment]::NewLine + [Environment]::NewLine + 'Open the Java download page now? Install Java 21, then double-click the game again.'; Add-Type -AssemblyName System.Windows.Forms; $r=[System.Windows.Forms.MessageBox]::Show($m,'KALAPASTANGAN needs Java',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Information); if ($r -eq 'Yes') { Start-Process 'https://adoptium.net/temurin/releases/?os=windows&arch=x64&package=jdk' }"
goto :done

:checknojava
echo Java:  NOT FOUND - install Java 21 from https://adoptium.net/temurin/releases/
goto :done

:oldjava
if /i "%~1"=="--check" goto :checkoldjava
powershell -NoProfile -ExecutionPolicy Bypass -Command "$m='KALAPASTANGAN found an old Java installation that cannot run the game.' + [Environment]::NewLine + [Environment]::NewLine + 'Open the Java download page now? Install Java 21, then double-click the game again.'; Add-Type -AssemblyName System.Windows.Forms; $r=[System.Windows.Forms.MessageBox]::Show($m,'KALAPASTANGAN needs a newer Java',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Information); if ($r -eq 'Yes') { Start-Process 'https://adoptium.net/temurin/releases/?os=windows&arch=x64&package=jdk' }"
goto :done

:checkoldjava
echo Java:  "%JAVAW%" is version %JAVAVER% - too old, the game needs Java 8 or newer (21 recommended)
goto :done

:done
popd
endlocal
exit /b 0
