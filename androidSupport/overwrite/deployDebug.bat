cd ..
call "%SCAR_HOME%\scar.bat"
cd android\assets
if NOT exist "www" goto :OK_subDirCreate
rmdir /Q /S www
:OK_subDirCreate
mkdir www
cd ..
XCOPY ..\build\war assets\www /E /Y /EXCLUDE:%SCAR_HOME%\androidSupport\notWEB-INF.txt
call "%ANT_HOME%\ant.bat" debug
"%ANT_HOME%\ant.bat" installd
