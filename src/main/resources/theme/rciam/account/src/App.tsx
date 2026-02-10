import { Page, Spinner } from "@patternfly/react-core";
import style from "./App.module.css";
import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { PageNav } from "./PageNav";
import {
  AccountEnvironment,
  Header,
  useEnvironment,
} from "@keycloak/keycloak-account-ui";
import { AlertProvider } from "@keycloak/keycloak-ui-shared";

function App() {
  useEnvironment<AccountEnvironment>();
  return (
      <AlertProvider>
        <Page
          className={style.headerLogo}
          header={<Header />}
          sidebar={<PageNav />}
          isManagedSidebar
        >
          <Suspense fallback={<Spinner />}>
            <Outlet />
          </Suspense>
        </Page>
      </AlertProvider>
  );
}

export default App;
