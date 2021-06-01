# keycloak-theme-vanilla

This is a custom theme for eosc-kc installations

##Installation instructions:

Create the following folders:
$KEYCLOAK_BASE/modules/system/layers/keycloak/org/keycloak/keycloak-theme-vanilla
$KEYCLOAK_BASE/modules/system/layers/keycloak/org/keycloak/keycloak-theme-vanilla/main

and add into the folder "main" 
* the built jar keycloak-theme-vanilla/target/keycloak-theme-vanilla.jar
* the keycloak-theme-vanilla/module.xml from the 

so you should end up with the following structure in
$KEYCLOAK_BASE/modules/system/layers/keycloak/org/keycloak/keycloak-theme-vanilla

```
keycloak-theme-vanilla
└── main
    ├── keycloak-theme-vanilla.jar
    └── module.xml
```

Following the above, we should also let wildfly server and keycloak to load this module as well. 
So, open file $KEYCLOAK_BASE/standalone/configuration/standalone.xml

Find the <subsystem xmlns="urn:jboss:domain:keycloak-server:1.1"> node.

* Add the 
```<provider>module:org.keycloak.keycloak-theme-vanilla</provider>```
into the <providers> list
* Add the 
```
<modules>
    <module>org.keycloak.keycloak-theme-vanilla</module>
</modules>
```
into the ```<theme>``` block



