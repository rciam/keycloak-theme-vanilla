# Changelog
All notable changes in keycloak-theme-vanilla will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

For Keycloak upstream changelog please see https://www.keycloak.org/docs/latest/release_notes/index.html.
Full Keycloak upstream jira issue can be shown if filtered by Fix version. For example [Keycloak jira issue for 15.0.2 version](https://issues.redhat.com/browse/KEYCLOAK-19161?jql=project%20%3D%20keycloak%20and%20fixVersion%20%3D%2015.0.2)

## [v1.0.0] - 18-10-2021
### Added
- A configuration is created to allow loading custom project icon
- A default icon is added (GRNET icon)

## [v1.1.0] - 15-11-2021
### Added
- A footer is created to allow showing some teplated information
- Templated footer can now include icons, privacy policy urls, terms of use urls, contact emails, and a custom footer html snippet

## [v1.2.0] - 17-11-2021
### Added
- Updated configuration to enable defining a red ribbon for Demo instances 

### Changed
- Refactored the internal classes (changed package names, etc)

## [v2.0.0] - 13-12-2021
### Added
- Configuration file listeners and realm listeners, which update the running instance with any configuration changes (no restart required).
- Terms of use file and realm listeners, which update the running instance with any terms of use changes (no restart required).
- Also added an admin http interface to allow admins to upload a new config and terms of use on any realm.

### Changed
- Moved all deployment-specific resources (icons) in the vanilla. Configuration specifies which ones are used. 

## [v2.1.0] - 7-1-2022
### Changed
- The terms-of-use served in the terms.ftl now use an iframe to show the internal or external link configured.
- Theme config can now be partially updated from the REST endpoint. Previously, it could only be replaced.

## [v2.2.0] - 10-1-2022
### Changed
- Turned the theme into a hot-deployable theme.


## [v2.3.0] - 16-2-2022
### Changed
- Reassembled the header (project icon) and footer to be applied on all template-inheriting login ftl pages. 
- Extended the OIDC consent screen (RCIAM-791)