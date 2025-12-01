import { Page, Spinner } from "@patternfly/react-core";
import {
  AccountEnvironment,
  Header,
  PageNav,
  useEnvironment,
} from "@keycloak/keycloak-account-ui";
import { Suspense, useEffect, useState } from "react";
import { Outlet } from "react-router-dom";
import { Footer } from "./Footer";
import style from "./App.module.css";

type ThemeConfig = Record<string, string[]>;

export const useThemeConfig = () => {
    interface ExtendedEnvironment extends AccountEnvironment {
    authUrl: string;
  }

  const context = useEnvironment<ExtendedEnvironment>();
  const [config, setConfig] = useState<ThemeConfig | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;
    const realm = context.environment.realm;
    const serverBaseUrl = context.environment.authUrl;

    fetch(`${serverBaseUrl}realms/${realm}/theme-info/theme-config`, {
      signal,
      credentials: "include",
    })
      .then((res) => {
        if (!res.ok) {
          throw new Error(`Failed to load theme-config: ${res.status}`);
        }
        return res.json();
      })
      .then((data) => {
        setConfig(data);
      })
      .catch((e) => {
        if ((e as any).name !== "AbortError") {
          console.error("Error loading theme config", e);
          setError(e);
        }
      });

    return () => controller.abort();
  }, [context.environment.baseUrl, context.environment.realm]);
  return { config, error };
};

function App() {
  // const realm = context.environment.realm;
  const { config } = useThemeConfig();
  console.log(config);
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
      <Footer config={config} />
    </>
  );
}

export default App;
