// src/Footer.tsx
import React from "react";
import {
  PageSection,
  Grid,
  GridItem,
  Text,
  TextContent,
} from "@patternfly/react-core";
import {
  AccountEnvironment,
  useEnvironment,
} from "@keycloak/keycloak-account-ui";
import "./footer.css";

type ThemeConfig = Record<string, string[]>;

interface FooterProps {
  config: ThemeConfig | null;
}

export const Footer: React.FC<FooterProps> = ({ config }) => {
  const context = useEnvironment<AccountEnvironment>();

  // resourceUrl is injected by Keycloak (same thing your old JS used)
  const resourceUrl =
    (context.environment as any).resourceUrl?.replace(/\/$/, "") ?? "";

  const resolveImgUrl = (path: string | undefined) => {
    if (!path) return "";
    if (/^https?:\/\//i.test(path)) {
      // already absolute
      return path;
    }
    if (!resourceUrl) {
      // fallback: relative (will likely be wrong, but avoids crashing)
      return path;
    }
    // /realms/<realm>/account/resources/rciam + /additional/grnet/logo.png
    return `${resourceUrl}/${path.replace(/^\//, "")}`;
  };

  if (!config) {
    return null;
  }

  const footerIconUrls = config.footerIconUrls ?? [];
  const htmlFooterText = config.htmlFooterText?.[0] ?? "";
  const privacyPolicyUrl = config.privacyPolicyUrl?.[0];
  const termsOfUseUrl = config.termsOfUseUrl?.[0];
  const supportUrl = config.supportUrl?.[0];
  const ribbonText = config.ribbonText?.[0];

  return (
    <>
      {ribbonText && <div className="corner-ribbon">{ribbonText}</div>}

      <PageSection
        id="footer"
        className="rciam-footer"
        variant="default"
        isFilled={false}
      >
        <Grid hasGutter>
          <GridItem span={4}>
            <div className="rciam-footer-support">
              {supportUrl && (
                <a
                  className="horizontal-padding-10"
                  href={supportUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  Support
                </a>
              )}
            </div>
          </GridItem>

          <GridItem span={4}>
            <div className="rciam-footer-logos">
              {footerIconUrls.map((relPath, idx) => {
                const src = resolveImgUrl(relPath);
                if (!src) return null;
                return (
                  <img
                    key={idx}
                    src={src}
                    className="horizontal-padding-10"
                    style={{ maxHeight: 50, margin: "auto" }}
                    alt=""
                  />
                );
              })}
            </div>
          </GridItem>

          <GridItem span={4}>
            <div className="rciam-footer-links">
              {privacyPolicyUrl && (
                <a
                  className="horizontal-padding-10"
                  href={privacyPolicyUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  Privacy
                </a>
              )}
              {termsOfUseUrl && (
                <a
                  className="horizontal-padding-10"
                  href={termsOfUseUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  Terms
                </a>
              )}
            </div>
          </GridItem>
        </Grid>

        <TextContent className="rciam-footer-text-wrapper">
          <Text
            id="footer-html-text"
            component="small"
            dangerouslySetInnerHTML={{ __html: htmlFooterText }}
          />
        </TextContent>
      </PageSection>
    </>
  );
};
