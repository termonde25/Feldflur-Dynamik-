@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  feldflur-dynamik startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables, and ensure extensions are enabled
setlocal EnableExtensions

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and FELDFLUR_DYNAMIK_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

1>&2 echo.
1>&2 echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
1>&2 echo.
1>&2 echo Please set the JAVA_HOME variable in your environment to match the
1>&2 echo location of your Java installation.

"%COMSPEC%" /c exit 1
goto exitWithErrorLevel

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

1>&2 echo.
1>&2 echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
1>&2 echo.
1>&2 echo Please set the JAVA_HOME variable in your environment to match the
1>&2 echo location of your Java installation.

"%COMSPEC%" /c exit 1
goto exitWithErrorLevel

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\feldflur-dynamik.jar;%APP_HOME%\lib\libmicro-5.0-beta16.jar;%APP_HOME%\lib\bdi-5.0-beta16.jar;%APP_HOME%\lib\classreader-5.0-beta16.jar;%APP_HOME%\lib\requiredservice-5.0-beta16.jar;%APP_HOME%\lib\providedservice-5.0-beta16.jar;%APP_HOME%\lib\injection-5.0-beta16.jar;%APP_HOME%\lib\execution-5.0-beta16.jar;%APP_HOME%\lib\result-5.0-beta16.jar;%APP_HOME%\lib\logbase-5.0-beta16.jar;%APP_HOME%\lib\core-5.0-beta16.jar;%APP_HOME%\lib\future-5.0-beta16.jar;%APP_HOME%\lib\traverser-5.0-beta16.jar;%APP_HOME%\lib\idgenerator-5.0-beta16.jar;%APP_HOME%\lib\javaparser-5.0-beta16.jar;%APP_HOME%\lib\bytecode-5.0-beta16.jar;%APP_HOME%\lib\collection-5.0-beta16.jar;%APP_HOME%\lib\common-5.0-beta16.jar;%APP_HOME%\lib\gdx-backend-lwjgl3-1.14.2.jar;%APP_HOME%\lib\gdx-1.14.2.jar;%APP_HOME%\lib\gdx-platform-1.14.2-natives-desktop.jar;%APP_HOME%\lib\byte-buddy-1.17.8.jar;%APP_HOME%\lib\asm-util-9.9.jar;%APP_HOME%\lib\asm-commons-9.9.jar;%APP_HOME%\lib\asm-analysis-9.9.jar;%APP_HOME%\lib\asm-tree-9.9.jar;%APP_HOME%\lib\asm-9.9.jar;%APP_HOME%\lib\nano-cuckoo-3.0.2.jar;%APP_HOME%\lib\gdx-jnigen-loader-2.5.2.jar;%APP_HOME%\lib\lwjgl-glfw-3.3.3.jar;%APP_HOME%\lib\lwjgl-glfw-3.3.3-natives-linux.jar;%APP_HOME%\lib\lwjgl-glfw-3.3.3-natives-linux-arm32.jar;%APP_HOME%\lib\lwjgl-glfw-3.3.3-natives-linux-arm64.jar;%APP_HOME%\lib\lwjgl-glfw-3.3.3-natives-macos.jar;%APP_HOME%\lib\lwjgl-glfw-3.3.3-natives-macos-arm64.jar;%APP_HOME%\lib\lwjgl-glfw-3.3.3-natives-windows.jar;%APP_HOME%\lib\lwjgl-glfw-3.3.3-natives-windows-x86.jar;%APP_HOME%\lib\lwjgl-jemalloc-3.3.3.jar;%APP_HOME%\lib\lwjgl-jemalloc-3.3.3-natives-linux.jar;%APP_HOME%\lib\lwjgl-jemalloc-3.3.3-natives-linux-arm32.jar;%APP_HOME%\lib\lwjgl-jemalloc-3.3.3-natives-linux-arm64.jar;%APP_HOME%\lib\lwjgl-jemalloc-3.3.3-natives-macos.jar;%APP_HOME%\lib\lwjgl-jemalloc-3.3.3-natives-macos-arm64.jar;%APP_HOME%\lib\lwjgl-jemalloc-3.3.3-natives-windows.jar;%APP_HOME%\lib\lwjgl-jemalloc-3.3.3-natives-windows-x86.jar;%APP_HOME%\lib\lwjgl-openal-3.3.3.jar;%APP_HOME%\lib\lwjgl-openal-3.3.3-natives-linux.jar;%APP_HOME%\lib\lwjgl-openal-3.3.3-natives-linux-arm32.jar;%APP_HOME%\lib\lwjgl-openal-3.3.3-natives-linux-arm64.jar;%APP_HOME%\lib\lwjgl-openal-3.3.3-natives-macos.jar;%APP_HOME%\lib\lwjgl-openal-3.3.3-natives-macos-arm64.jar;%APP_HOME%\lib\lwjgl-openal-3.3.3-natives-windows.jar;%APP_HOME%\lib\lwjgl-openal-3.3.3-natives-windows-x86.jar;%APP_HOME%\lib\lwjgl-opengl-3.3.3.jar;%APP_HOME%\lib\lwjgl-opengl-3.3.3-natives-linux.jar;%APP_HOME%\lib\lwjgl-opengl-3.3.3-natives-linux-arm32.jar;%APP_HOME%\lib\lwjgl-opengl-3.3.3-natives-linux-arm64.jar;%APP_HOME%\lib\lwjgl-opengl-3.3.3-natives-macos.jar;%APP_HOME%\lib\lwjgl-opengl-3.3.3-natives-macos-arm64.jar;%APP_HOME%\lib\lwjgl-opengl-3.3.3-natives-windows.jar;%APP_HOME%\lib\lwjgl-opengl-3.3.3-natives-windows-x86.jar;%APP_HOME%\lib\lwjgl-stb-3.3.3.jar;%APP_HOME%\lib\lwjgl-stb-3.3.3-natives-linux.jar;%APP_HOME%\lib\lwjgl-stb-3.3.3-natives-linux-arm32.jar;%APP_HOME%\lib\lwjgl-stb-3.3.3-natives-linux-arm64.jar;%APP_HOME%\lib\lwjgl-stb-3.3.3-natives-macos.jar;%APP_HOME%\lib\lwjgl-stb-3.3.3-natives-macos-arm64.jar;%APP_HOME%\lib\lwjgl-stb-3.3.3-natives-windows.jar;%APP_HOME%\lib\lwjgl-stb-3.3.3-natives-windows-x86.jar;%APP_HOME%\lib\lwjgl-3.3.3.jar;%APP_HOME%\lib\lwjgl-3.3.3-natives-linux.jar;%APP_HOME%\lib\lwjgl-3.3.3-natives-linux-arm32.jar;%APP_HOME%\lib\lwjgl-3.3.3-natives-linux-arm64.jar;%APP_HOME%\lib\lwjgl-3.3.3-natives-macos.jar;%APP_HOME%\lib\lwjgl-3.3.3-natives-macos-arm64.jar;%APP_HOME%\lib\lwjgl-3.3.3-natives-windows.jar;%APP_HOME%\lib\lwjgl-3.3.3-natives-windows-x86.jar;%APP_HOME%\lib\jlayer-1.0.1-gdx.jar;%APP_HOME%\lib\jorbis-0.0.17.jar;%APP_HOME%\lib\javapoet-1.13.0.jar;%APP_HOME%\lib\lz4-1.3.0.jar;%APP_HOME%\lib\nativetools-5.0-beta16.jar;%APP_HOME%\lib\jna-5.18.1.jar


@rem Execute feldflur-dynamik
@rem endlocal doesn't take effect until after the line is parsed and variables are expanded
@rem which allows us to clear the local environment before executing the java command
endlocal & "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %FELDFLUR_DYNAMIK_OPTS%  -classpath "%CLASSPATH%" feldflur.HelloAgent %* & call :exitWithErrorLevel & goto exitWithErrorLevel

@rem This label must not be changed. We rely on old scripts being able to jump to this point.
:exitWithErrorLevel
@rem Use "%COMSPEC%" /c exit to allow operators to work properly in scripts
"%COMSPEC%" /c exit %ERRORLEVEL%
