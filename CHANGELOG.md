# Changelog
All notable changes in keycloak-theme-vanilla will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

For Keycloak upstream changelog please see https://www.keycloak.org/docs/latest/release_notes/index.html.
  Full Keycloak upstream jira issue can be shown if filtered by Fix version. For example [Keycloak jira issue for 15.0.2 version](https://issues.redhat.com/browse/KEYCLOAK-19161?jql=project%20%3D%20keycloak%20and%20fixVersion%20%3D%2015.0.2)

## [5.6.2] - 2025-04-01
### Added
- Unique Identifier Field add to Update Profile Login Page

## [5.6.1] - 2025-03-21

### Fixed
- Fix bug when a last login IdP is disabled or removed

## [5.6.0] - 2025-01-21
### Added
- Hide username based on configuration during first broker login
- Add message in search IdP placeholder

## [5.5.0] - 2024-12-12
### Changed
- Change update profile pages

## [5.4.0] - 2024-26-11
### Changed
- Always show search for IdPs in login page
- Fix Dublicate Idps

## [5.3.2] - 2024-11-14
### Changed
- Change ui for last login IdPs

## [5.3.1] - 2024-10-31
### Fixed
- Fix ui login problem

## [5.3.0] - 2024-10-25
Remains compatible with Keycloak 22.0.11-XXXX but requires Keycloak 22.0.11-1.11 or later to enable showing chosen IdPs.
### Added
- Show IdP logos from logoUri
- Show chosen IdPs

## [5.2.0] - 2024-06-17
### Changed
- Promoted IdPs change in WAYF

## [5.1.1] - 2024-06-13
### Changed
- Simplify WAYF IdPs search (addition)

## [5.1.0] - 2024-06-12
### Changed
- Simplify WAYF IdPs search

## [5.0.3] - 2024-05-29
### Changed
- GRNET specific 'Terms and Conditions' - Update Account

## [5.0.2] - 2024-05-17
### Changed
- Change account message username to 'User Identifier'
- Change terms and conditions before User register

## [v5.0.1] - 2023-11-03
### Fixed
- Correct baseUri in template.ftl

## [v5.0.0] - 2023-11-02
### Changed
- Support for Keycloak version 22.0.3 with Java Jakarta 17

## [v4.1.2] - 2022-09-27
### Changed
- Increased the AUP width, the WAYF listing width and removed the logo extra padding (RCIAM-1124).
- Bullets in claims (release) listing on client consent screen (RCIAM-1124).
- Various changes on elements padding in OIDC client consent screen (RCIAM-1124).
- Client terms UI enhancements, claims list bold, left padding ul, changed oauthGrantRequest text message (RCIAM-1124).

## [v4.1.1] - 2022-07-20
### Changed
- Enhanced the login page IdP listing (css changes)

## [v4.1.0] - 2022-06-02
### Changed
- Searches also for QUARKUS_HOME as an alternative for app's base path
- Stores config files under <application_server>/theme-config path

## [v4.0.2] - 2022-05-19
### Added
- Spinner while searching/filtering IdPs in WAYF
### Changed
- Bugfix RCIAM-738: Fixes duplicate entry listing in WAYF search as you type

## [v4.0.1] - 2022-05-19
### Changed
- Bugfix - In the WAYF IdP listing, if in the first resultset there is at least one hidden IdP, the search bar becomes hidden.

## [v4.0.0] - 2022-05-16
### Added
The theme now has the ability to serve static files (of any type), like a webserver. More information can be found in the README.md file.
In the WAYF page, the search fetches any matches, without respect to any special characters or upper/lower case. i.e. searching 'muller' would also match to anything with 'Müller' within  

## [v3.0.1] - 2022-03-07
### Changed
- Bugfix - On account pages, the uppermost left banner icon, if larger than a certain css size, would rescale to zero size (0x0 pixels).

## [v3.0.0] - 2022-02-24
### Added
- Theme now was extended to also modify the user account management pages. It now offers the option to modify (per-realm) the footer and the logo icons.

## [v2.3.0] - 2022-02-16
### Changed
- Reassembled the header (project icon) and footer to be applied on all template-inheriting login ftl pages.
- Extended the OIDC consent screen (RCIAM-791)

## [v2.2.0] - 2022-01-10
### Changed
- Turned the theme into a hot-deployable theme.

## [v2.1.0] - 2022-01-07
### Changed
- The terms-of-use served in the terms.ftl now use an iframe to show the internal or external link configured.
- Theme config can now be partially updated from the REST endpoint. Previously, it could only be replaced.

## [v2.0.0] - 2021-12-13
### Added
- Configuration file listeners and realm listeners, which update the running instance with any configuration changes (no restart required).
- Terms of use file and realm listeners, which update the running instance with any terms of use changes (no restart required).
- Also added an admin http interface to allow admins to upload a new config and terms of use on any realm.
### Changed
- Moved all deployment-specific resources (icons) in the vanilla. Configuration specifies which ones are used.

## [v1.2.0] - 2021-11-17
### Added
- Updated configuration to enable defining a red ribbon for Demo instances
### Changed
- Refactored the internal classes (changed package names, etc)

## [v1.1.0] - 2021-11-15
### Added
- A footer is created to allow showing some teplated information
- Templated footer can now include icons, privacy policy urls, terms of use urls, contact emails, and a custom footer html snippet

## [v1.0.0] - 2021-10-18
### Added
- A configuration is created to allow loading custom project icon
- A default icon is added (GRNET icon)













