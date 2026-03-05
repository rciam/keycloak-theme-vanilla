import type { IndexRouteObject, RouteObject } from "react-router-dom";
import App from "./App";
import { environment } from "./environment";
import {
  DeviceActivity,
  Oid4Vci,
  Resources
} from "@keycloak/keycloak-account-ui";
import { LinkedAccounts } from "./account-security/LinkedAccounts";
import { Applications } from "./applications/Applications";
import { SigningIn } from "./signing-in/SigningIn";
import { PersonalInfo } from "./personal-info/PersonalInfo";
// We can define a small extension type for convenience
export type NavRouteObject = RouteObject & {
  handle?: {
    navGroupId?: string; // which sidebar group this route belongs to
    navGroupLabelKey?: string; // optional custom i18n key for the group
    navItemLabelKey?: string; // optional custom i18n key for this item
    hideFromNav?: boolean; // optional: don't show in sidebar at all
  };
};

export const DeviceActivityRoute: NavRouteObject = {
  path: "account-security/device-activity",
  element: <DeviceActivity />,
};

export const LinkedAccountsRoute: NavRouteObject = {
  path: "account-security/linked-accounts",
  element: <LinkedAccounts />,
};

export const SigningInRoute: NavRouteObject = {
  path: "account-security/signing-in",
  element: <SigningIn />,
};

export const ApplicationsRoute: NavRouteObject = {
  path: "applications",
  element: <Applications />,
};

export const ResourcesRoute: NavRouteObject = {
  path: "resources",
  element: <Resources />,
};

export const PersonalInfoRoute: IndexRouteObject & NavRouteObject = {
  index: true,
  element: <PersonalInfo />,
  path: "",
};

export const Oid4VciRoute: NavRouteObject = {
  path: "oid4vci",
  element: <Oid4Vci />,
};

export const RootRoute: NavRouteObject = {
  path: decodeURIComponent(new URL(environment.baseUrl).pathname),
  element: <App />,
  errorElement: <>Error</>,
  children: [
    PersonalInfoRoute,
    DeviceActivityRoute,
    LinkedAccountsRoute,
    SigningInRoute,
    ApplicationsRoute,
    ResourcesRoute,
    ...(environment.features.isOid4VciEnabled ? [Oid4VciRoute] : []),
  ],
};

export const routes: NavRouteObject[] = [RootRoute];
