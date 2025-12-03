import { Page, Spinner } from "@patternfly/react-core";
import { Header, PageNav } from "@keycloak/keycloak-account-ui";
import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import style from "./App.module.css";

function App() {
  return (
    <>
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
    </>
  );
}

export default App;
