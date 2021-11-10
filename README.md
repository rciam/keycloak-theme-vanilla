# keycloak-theme-vanilla

This is the vanilla theme for rciam installations.

:red_circle: **IMPORTANT**: 
- In this repo, the master branch contains the base code (and a default template)
- Project-specific customizations "inherit" the master's code (builds the master) and add the customizations they include. These should reside in a different project-dedicated repository. For instance, the DISSCO project has its customizations in its own repo, [here](https://github.com/grnet/keycloak-theme-dissco)

    
## Installation instructions:

Create the following folders:
$KEYCLOAK_BASE/modules/system/layers/keycloak/org/keycloak/keycloak-theme-vanilla
$KEYCLOAK_BASE/modules/system/layers/keycloak/org/keycloak/keycloak-theme-vanilla/main

and add into the folder "main" 
* the built jar keycloak-theme-vanilla/target/keycloak-theme-vanilla.jar
* the keycloak-theme-vanilla/module.xml from the source (this one) base folder

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

Find the ```<subsystem xmlns="urn:jboss:domain:keycloak-server:1.1">``` node.

* Add the 
```<provider>module:org.keycloak.keycloak-theme-vanilla</provider>```
into the ```<providers>``` list
* Add the 
    ```
    <modules>
        <module>org.keycloak.keycloak-theme-vanilla</module>
    </modules>
    ```
    into the ```<theme>``` block

## Compatibility matrix

|  Theme version | Keycloak version |
|---|---|
|  v1.0.0 | v15.0.2-r1.0.1+ |


## License

* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)



