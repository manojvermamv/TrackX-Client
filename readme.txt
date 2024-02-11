
STEP 1. Use Java keytool to convert from JKS to P12...

    Command: keytool -importkeystore -srckeystore keystore.jks -destkeystore keystore.p12 -deststoretype PKCS12 -srcalias <jkskeyalias> -deststorepass <password> -destkeypass <password>

    Note:   Export from keytool's proprietary format (called "JKS") to standardized format PKCS #12

STEP 2. Export certificate (P12 to PEM) using openssl - Dump the new pkcs12 file into pem

    Command (public key - encrypted):           openssl pkcs12 -in keystore.p12 -nokeys -out certificate.pem
    Command (private key - unencrypted):        openssl pkcs12 -in keystore.p12 -nodes -nocerts -out certificate.pem
    Command (certificate & private rsa key):    openssl pkcs12 -in keystore.p12 -nodes -out certificate.pem

    Note:   You should have both the cert and private key in pem format. Split them up.
            Put the part between “BEGIN CERTIFICATE” and “END CERTIFICATE” into cert.x509.pem
            Put the part between “BEGIN RSA PRIVATE KEY” and “END RSA PRIVATE KEY” into private.rsa.pem


STEP 3: Convert Private RSA PEM to PKCS8

    Command: openssl pkcs8 -topk8 -outform DER -in private.rsa.pem -inform PEM -out private.pk8 -nocrypt

    Note:   This convert the private key into pk8 format as expected by signapk
            Use cert.x509.pem and private.pk8 to sign apks

STEP 4: Convert x509 Certificate to certificate.cer

    Command: openssl x509 -in cert.x509.pem -outform pem -outform der -out cert.cer

    Note:   Use this command if you want to see as text form
            openssl x509 -inform der -in cert.cer -noout -text

Extra:
    Download Links: You can download OpenSSL for windows 32 and 64 bit from the respective links below
    http://code.google.com/p/openssl-for-windows/downloads/detail?name=openssl-0.9.8k_X64.zip
    https://code.google.com/p/openssl-for-windows/downloads/detail?name=openssl-0.9.8k_WIN32.zip




------------------------------------------------------------------------------------------------>
                    Sign android app with platform keys using gradle
------------------------------------------------------------------------------------------------>
https://itecnote.com/tecnote/android-how-to-sign-android-app-with-platform-keys-using-gradle/
------------------------------------------------------------------------------------------------>

1) Generate your Platform Keystore (.keystore) file from your Java Keystore (.jks)

$ keytool -importkeystore -destkeystore platform.keystore -deststorepass password -srckeystore keystore.jks -srcstorepass password


2) Test your new keystore:

$ jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore platform.keystore -storepass password \app\build\outputs\apk\release\app-unsigned.apk alias


3) Deploy keystore in your gradle build:

signingConfigs {
 debug {
    storeFile     file('debug.keystore')
    storePassword 'android'
    keyAlias      'androiddebugkey'
    keyPassword   'android'
 }
 release {
    storeFile     file('platform.keystore')
    storePassword 'password'
    keyAlias      'alias'
    keyPassword   'password'
 }
}
The above build.gradle is also showing an example of using the android debug keystore as standard for debug builds.

