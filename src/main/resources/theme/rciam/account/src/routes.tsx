import type { IndexRouteObject, RouteObject } from "react-router-dom";
import App from "./App";
import { environment } from "./environment";
import {
  Applications,
  DeviceActivity,
  Groups,
  LinkedAccounts,
  Oid4Vci,
  Resources,
  ContentComponent,
  SigningIn,
  PersonalInfo
} from "@keycloak/keycloak-account-ui";
// /import { MyPage } from "./MyPage";
export const DeviceActivityRoute: RouteObject = {
  path: "account-security/device-activity",
  element: <DeviceActivity />,
};

export const LinkedAccountsRoute: RouteObject = {
  path: "account-security/linked-accounts",
  element: <LinkedAccounts />,
};

export const SigningInRoute: RouteObject = {
  path: "account-security/signing-in",
  element: <SigningIn />,
};

export const ApplicationsRoute: RouteObject = {
  path: "applications",
  element: <Applications />,
};

export const GroupsRoute: RouteObject = {
  path: "groups",
  element: <Groups />,
};

export const ResourcesRoute: RouteObject = {
  path: "resources",
  element: <Resources />,
};

export type ContentComponentParams = {
  componentId: string;
};

export const ContentRoute: RouteObject = {
  path: "content/:componentId",
  element: <ContentComponent />,
};

export const PersonalInfoRoute: IndexRouteObject = {
  index: true,
  element: <PersonalInfo />,
  path: "",
};

export const Oid4VciRoute: RouteObject = {
  path: "oid4vci",
  element: <Oid4Vci />,
};

export const RootRoute: RouteObject = {
  path: decodeURIComponent(new URL(environment.baseUrl).pathname),
  element: <App />,
  errorElement: <>Error</>,
  children: [
    PersonalInfoRoute,
    DeviceActivityRoute,
    LinkedAccountsRoute,
    SigningInRoute,
    ApplicationsRoute,
    GroupsRoute,
    PersonalInfoRoute,
    ResourcesRoute,
    ContentRoute,
    ...(environment.features.isOid4VciEnabled ? [Oid4VciRoute] : []),
  ],
};

export const routes: RouteObject[] = [RootRoute];
