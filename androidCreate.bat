@echo off

echo .
echo . androidCreate vs 1.0 w/ '%1'
echo .

if exist "%JAVA_HOME%\bin" goto :OK_JAVA

:QuestionableJAVA
echo . JAVA_HOME does not appear to be set up correctly!
goto :exit

:OK_JAVA

if NOT exist "%SCAR_HOME%\androidSupport\name\Start.txt" goto :QuestionableSCAR
if NOT exist "%SCAR_HOME%\androidSupport\name\End.txt" goto :QuestionableSCAR
if NOT exist "%SCAR_HOME%\androidSupport\overwrite\AndroidManifest.xml" goto :QuestionableSCAR
if exist "%SCAR_HOME%\scar.bat" goto :OK_SCAR

:QuestionableSCAR
echo . SCAR_HOME does not appear to be set up correctly!
goto :exit

:OK_SCAR

if exist "%ANT_HOME%\ant.bat" goto :OK_ANT

:QuestionableANT
echo . ANT_HOME does not appear to be set up correctly!
goto :exit

:OK_ANT

if NOT exist "build.yaml" goto :QuestionableProjectDir
if exist "src" goto :OK_projectDir

:QuestionableProjectDir
echo . "build.yaml" or "src" not found, suspect not in project dir to host PhoneGap android subdirectory project!
goto :exit

:OK_projectDir

if "%1"=="" goto :help
if "%2"=="" goto :OK_params

:help
echo . Help...
echo .
echo . To run "androidCreate" you must provide one and only one parameter:
echo .
echo .        Android Application's Name.
echo .
echo . Note: this script must be allowed to manipulate the current directory
echo . such that a clean "android" sub-directory can be created and then
echo . populated appropriately with the Android infrastructure for a PhoneGap
echo . Android subdirectory project!
goto :exit

:OK_params

if NOT exist "android" goto :OK_subDirCreate

:Purge_androidSubDir

rmdir /Q /S android

if NOT exist "android" goto :OK_subDirCreate

:Unable_Purge_androidSubDir
echo . Unable to remove the "android" sub-directory - probably in "use"!
goto :exit

:OK_subDirCreate

mkdir android

if exist "android" goto :OK_Populate

:Unable_Create_androidSubDir
echo . Unable to create the "android" sub-directory!
goto :exit

:OK_Populate

cd android

call android create project --target 1 --path . --activity PhoneGapActivity --package org.litesoft.droid --name "%1"

echo .

echo %1 > ProjectName.txt

if exist res\values\strings.xml goto :OK_androided

:Unable_androided
echo . android command did not work!
goto :exit

:OK_androided

del res\values\strings.xml

copy "%SCAR_HOME%\androidSupport\name\Start.txt"+ProjectName.txt+"%SCAR_HOME%\androidSupport\name\End.txt" res\values\strings.xml

XCOPY "%SCAR_HOME%\androidSupport\overwrite" /E /Y

echo .
echo . Done!

"\Program Files (x86)\TextPad 4\TextPad.exe" res\values\strings.xml
:exit
