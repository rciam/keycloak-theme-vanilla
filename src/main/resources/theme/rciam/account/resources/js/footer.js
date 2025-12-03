// resources/js/footer.js

(function initRciamFooter() {
  console.log("rciam footer: bootstrap");

  function getEnvironment() {
    var el = document.getElementById("environment");
    if (!el) {
      console.error("rciam footer: #environment script tag not found");
      return null;
    }
    try {
      const json = el.textContent;
      console.log("rciam footer: environment JSON raw =", json);
      return JSON.parse(json);
    } catch (e) {
      console.error("rciam footer: failed to parse environment JSON", e);
      return null;
    }
  }

  var env = getEnvironment();
  if (!env) {
    return;
  }

  console.log("rciam footer: environment parsed =", env);

  // Values from index.ftl environment JSON
  var serverBaseUrl = env.serverBaseUrl; // e.g. http://localhost:8080/
  var realm = env.realm;                 // realm name
  var resourceUrl = (env.resourceUrl || "").replace(/\/$/, ""); // /realms/.../account/resources/rciam

  function resolveImgUrl(path) {
    if (!path) return "";
    if (/^https?:\/\//i.test(path)) {
      return path;
    }
    if (!resourceUrl) {
      return path;
    }
    // /realms/<realm>/account/resources/rciam + /additional/logo.png
    return resourceUrl + "/" + path.replace(/^\//, "");
  }

  function applyConfig(config) {
    console.log("rciam footer: config =", config);

    var footerIconUrls = config["footerIconUrls"] || [];
    var htmlFooterText = config["htmlFooterText"] || [];
    var privacyPolicyUrl = config["privacyPolicyUrl"] || [];
    var termsOfUseUrl = config["termsOfUseUrl"] || [];
    var supportUrl = config["supportUrl"] || [];
    var ribbonText = config["ribbonText"] || [];

    var logosContainer = document.querySelector("#footer-logos-container");
    var linksContainer = document.querySelector("#footer-links-container");
    var supportContainer = document.querySelector("#footer-support-container");
    var footerHtmlTextElem = document.querySelector("#footer-html-text");

    console.log("rciam footer: containers =",
      logosContainer,
      linksContainer,
      supportContainer,
      footerHtmlTextElem
    );

    if (!logosContainer || !linksContainer || !supportContainer || !footerHtmlTextElem) {
      console.warn("rciam footer: footer containers not found in DOM");
      return;
    }

    // DEBUG: show something no matter what
    footerHtmlTextElem.innerHTML = "DEBUG FOOTER – JS IS RUNNING";

    // Support link
    if (supportUrl[0]) {
      var supportElem = document.createElement("a");
      supportElem.className = "horizontal-padding-10";
      supportElem.href = supportUrl[0];
      supportElem.textContent = "Support";
      supportContainer.appendChild(supportElem);
    }

    // Logos
    footerIconUrls.forEach(function (iconPath) {
      if (!iconPath) return;
      var fullUrl = resolveImgUrl(iconPath);
      var logoElem = document.createElement("img");
      logoElem.src = fullUrl;
      logoElem.className = "horizontal-padding-10";
      logoElem.style.maxHeight = "50px";
      logoElem.style.margin = "auto";
      logosContainer.appendChild(logoElem);
    });

    // Privacy link
    if (privacyPolicyUrl[0]) {
      var privacyElem = document.createElement("a");
      privacyElem.className = "horizontal-padding-10";
      privacyElem.href = privacyPolicyUrl[0];
      privacyElem.textContent = "Privacy";
      linksContainer.appendChild(privacyElem);
    }

    // Terms link (with default)
    var defaultTOUUrl =
      serverBaseUrl.replace(/\/$/, "") +
      "/realms/" +
      realm +
      "/theme-info/terms-of-use";
    var termsUrl = termsOfUseUrl[0] || defaultTOUUrl;
    var termsElem = document.createElement("a");
    termsElem.className = "horizontal-padding-10";
    termsElem.href = termsUrl;
    termsElem.textContent = "Terms";
    linksContainer.appendChild(termsElem);

    // Footer HTML text (after debug)
    if (htmlFooterText[0]) {
      footerHtmlTextElem.innerHTML = htmlFooterText[0];
    }

    // Ribbon
    if (ribbonText[0]) {
      var ribbon = document.createElement("div");
      ribbon.className = "corner-ribbon";
      ribbon.textContent = ribbonText[0];
      document.body.appendChild(ribbon);
    }
  }

  function drawFooterInPlace() {
    console.log("rciam footer: drawing footer, fetching footer.html from", resourceUrl + "/elements/footer.html");

    fetch(resourceUrl + "/elements/footer.html", { credentials: "include" })
      .then(function (r) {
        console.log("rciam footer: footer.html status =", r.status);
        return r.text();
      })
      .then(function (html) {
        console.log("rciam footer: footer.html length =", html.length);
        // Instead of firstChild trick, insert as-is
        var wrapper = document.createElement("div");
        wrapper.id = "rciam-footer-wrapper";
        wrapper.innerHTML = html;
        document.body.appendChild(wrapper);
      })
      .then(function () {
        var url =
          serverBaseUrl.replace(/\/$/, "") +
          "/realms/" +
          realm +
          "/theme-info/theme-config";
        console.log("rciam footer: fetching theme-config from", url);
        return fetch(url, { credentials: "include" });
      })
      .then(function (response) {
        console.log("rciam footer: theme-config status =", response.status);
        return response.json();
      })
      .then(function (config) {
        applyConfig(config);
      })
      .catch(function (e) {
        console.error("rciam footer: error initializing footer", e);
      });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", drawFooterInPlace);
  } else {
    drawFooterInPlace();
  }
})();
