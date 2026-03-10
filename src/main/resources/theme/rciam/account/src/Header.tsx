import { KeycloakMasthead, label } from "@keycloak/keycloak-ui-shared";
import { useEnvironment } from "@keycloak/keycloak-account-ui";
import { useTranslation } from "react-i18next";
import style from "./header.module.css";
import { useEffect, useState } from "react";
import { Button } from "@patternfly/react-core";
import { ExternalLinkSquareAltIcon } from "@patternfly/react-icons";
import { isLocalUrl, joinPath } from "./js/utils";
import { AccountEnvironmentExtended } from "./environment";

type ThemeConfigResponse = {
  projectLogoIconUrl?: string[];
};

const ReferrerLink = () => {
  const { t } = useTranslation();
  const { environment } = useEnvironment<AccountEnvironmentExtended>();

  return environment.referrerUrl ? (
    <Button
      data-testid="referrer-link"
      component="a"
      href={environment.referrerUrl.replace("_hash_", "#")}
      variant="link"
      icon={<ExternalLinkSquareAltIcon />}
      iconPosition="right"
      isInline
    >
      {t("backTo", {
        app: label(t, environment.referrerName, environment.referrerUrl),
      })}
    </Button>
  ) : null;
};

export const Header = () => {
  const { environment, keycloak } =
    useEnvironment<AccountEnvironmentExtended>();
  const { t } = useTranslation();
  const [logo, setLogo] = useState<string>();

  const defaultLogo = `${environment.resourceUrl}/additional/logo.png`;

  useEffect(() => {
    const getThemeConfig = async () => {
      try {
        await keycloak.updateToken(30).catch(() => keycloak.login());

        const token = keycloak.token;
        if (!token) {
          setLogo(defaultLogo);
          return;
        }

        const base = environment.serverBaseUrl.replace(/\/$/, "");
        const themeConfigUrl = `${base}/realms/${environment.realm}/theme-info/theme-config`;

        const response = await fetch(themeConfigUrl, {
          method: "GET",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          setLogo(defaultLogo);
          return;
        }

        let data: ThemeConfigResponse | undefined;
        try {
          data = (await response.json()) as ThemeConfigResponse;
        } catch {
          data = undefined;
        }

        const logoUrl = data?.projectLogoIconUrl?.[0];
        if (logoUrl) {
          setLogo(
            isLocalUrl(logoUrl)
              ? joinPath(environment.resourceUrl, logoUrl)
              : logoUrl,
          );
        } else {
          setLogo(defaultLogo);
        }
      } catch {
        setLogo(defaultLogo);
      }
    };

    getThemeConfig();
  }, [environment.realm, environment.resourceUrl, environment.serverBaseUrl, keycloak, defaultLogo]);

  return (
    <KeycloakMasthead
      data-testid="page-header"
      keycloak={keycloak}
      features={{ hasManageAccount: false }}
      brand={{
        href: environment.baseUrl,
        src: logo ?? defaultLogo,
        alt: t("logo"),
        className: style.brand,
      }}
      toolbarItems={[<ReferrerLink key="link" />]}
    />
  );
};