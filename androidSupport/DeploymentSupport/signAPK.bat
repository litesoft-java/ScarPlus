"%JAVA_HOME%\bin\jarsigner" -verbose -keystore keys/%1.keystore -signedjar %1_signed.apk c:bin\%1-release-unsigned.apk %1.keystore
