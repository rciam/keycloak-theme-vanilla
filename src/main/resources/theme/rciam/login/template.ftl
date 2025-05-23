<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false showAnotherWayIfPresent=true showUsernameHeader=true>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" class="${properties.kcHtmlClass!}"<#if realm.internationalizationEnabled> lang="${locale.currentLanguageTag}"</#if>>

<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
</head>

<body class="${properties.kcBodyClass!}">
<div class="${properties.kcLoginClass!}">
    <div id="kc-header" class="${properties.kcHeaderClass!}">
        <div id="kc-header-wrapper"
             class="${properties.kcHeaderWrapperClass!}">${kcSanitize(msg("loginTitleHtml",(realm.displayNameHtml!'')))?no_esc}</div>
    </div>
    <div class="${properties.kcFormCardClass!}">
        <header class="${properties.kcFormHeaderClass!}">
            <#if realm.internationalizationEnabled  && locale.supported?size gt 1>
                <div class="${properties.kcLocaleMainClass!}" id="kc-locale">
                    <div id="kc-locale-wrapper" class="${properties.kcLocaleWrapperClass!}">
                        <div id="kc-locale-dropdown" class="${properties.kcLocaleDropDownClass!}">
                            <a href="#" id="kc-current-locale-link">${locale.current}</a>
                            <ul class="${properties.kcLocaleListClass!}">
                                <#list locale.supported as l>
                                    <li class="${properties.kcLocaleListItemClass!}">
                                        <a class="${properties.kcLocaleItemClass!}" href="${l.url}">${l.label}</a>
                                    </li>
                                </#list>
                            </ul>
                        </div>
                    </div>
                </div>
            </#if>
            <#if showUsernameHeader>
                <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
                    <#if displayRequiredFields>
                        <div class="${properties.kcContentWrapperClass!}">
                            <div class="${properties.kcLabelWrapperClass!} subtitle">
                                <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                            </div>
                            <div class="col-md-10">
                                <h1 id="kc-page-title"><#nested "header"></h1>
                            </div>
                        </div>
                    <#else>
                        <h1 id="kc-page-title"><#nested "header"></h1>
                    </#if>
                <#else>
                    <#if displayRequiredFields>
                        <div class="${properties.kcContentWrapperClass!}">
                            <div class="${properties.kcLabelWrapperClass!} subtitle">
                                <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                            </div>
                            <div class="col-md-10">
                                <#nested "show-username">
                                <div id="kc-username" class="${properties.kcFormGroupClass!}">
                                    <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                                    <a id="reset-login" href="${url.loginRestartFlowUrl}">
                                        <div class="kc-login-tooltip">
                                            <i class="${properties.kcResetFlowIcon!}"></i>
                                            <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                                        </div>
                                    </a>
                                </div>
                            </div>
                        </div>
                    <#else>
                        <#nested "show-username">
                        <div id="kc-username" class="${properties.kcFormGroupClass!}">
                            <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                            <a id="reset-login" href="${url.loginRestartFlowUrl}">
                                <div class="kc-login-tooltip">
                                    <i class="${properties.kcResetFlowIcon!}"></i>
                                    <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                                </div>
                            </a>
                        </div>
                    </#if>
                </#if>
            </#if>
        </header>
        <div id="kc-content">
            <div id="kc-content-wrapper">

          <#-- App-initiated actions should not see warning messages about the need to complete the action -->
          <#-- during login.                                                                               -->
          <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
              <div class="alert-${message.type} ${properties.kcAlertClass!} pf-m-<#if message.type = 'error'>danger<#else>${message.type}</#if>">
                  <div class="pf-c-alert__icon">
                      <#if message.type = 'success'><span class="${properties.kcFeedbackSuccessIcon!}"></span></#if>
                      <#if message.type = 'warning'><span class="${properties.kcFeedbackWarningIcon!}"></span></#if>
                      <#if message.type = 'error'><span class="${properties.kcFeedbackErrorIcon!}"></span></#if>
                      <#if message.type = 'info'><span class="${properties.kcFeedbackInfoIcon!}"></span></#if>
                  </div>
                      <span class="${properties.kcAlertTitleClass!}">${kcSanitize(message.summary)?no_esc}</span>
              </div>
          </#if>

          <#nested "form">

            <#if auth?has_content && auth.showTryAnotherWayLink() && showAnotherWayIfPresent>
                <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
                    <div class="${properties.kcFormGroupClass!}">
                        <input type="hidden" name="tryAnotherWay" value="on"/>
                        <a href="#" id="try-another-way"
                           onclick="document.forms['kc-select-try-another-way-form'].submit();return false;">${msg("doTryAnotherWay")}</a>
                    </div>
                </form>
            </#if>

          <#if displayInfo>
              <div id="kc-info" class="${properties.kcSignUpClass!}">
                  <div id="kc-info-wrapper" class="${properties.kcInfoAreaWrapperClass!}">
                      <#nested "info">
                  </div>
              </div>
          </#if>
        </div>
      </div>

    </div>
  </div>

  <script>

    var realm = '${realm.name}';
    var baseUri = '${uriInfo.baseUri}';
    var resourcesCommonPath = '${url.resourcesCommonPath}';
    var resourcesPath = '${url.resourcesPath}';


    if(baseUri.endsWith("/"))
        baseUri = baseUri.substring(0,baseUri.lastIndexOf("/")); //remove the trailing slash
    var baseUriOrigin = new URL(baseUri).origin;


    function getConfig() {
        fetch(baseUri + '/realms/' + realm + '/theme-info/theme-config').then(response => response.json()).then(config => {
            applyConfig(config);
        });
    }


    function applyConfig(config){

        //set main logo (it's single config entry)
        var projectLogoIconUrl = config['projectLogoIconUrl'][0];
        var fullUrl = projectLogoIconUrl;
        if(!projectLogoIconUrl.trim().startsWith('http')){ //it's local path
            fullUrl = baseUriOrigin + resourcesPath + "/" + projectLogoIconUrl;
        }
        var image = createElementFromHTML("<img src='" + fullUrl + "' alt='" + realm + "' style='max-height:100px; width:auto;'>")
        var logoParentDiv = document.querySelector('#kc-header-wrapper');
        logoParentDiv.appendChild(image);

        //set footer icons/logos urls (multiple config entries)
        var iconUrls = config['footerIconUrls'];
        var logosContainerElem = document.querySelector('#footer-logos-container');
        if(iconUrls != null && iconUrls.length > 0){
            for (let i = 0; i < iconUrls.length; i++) {
                var iconUrl = iconUrls[i];
                if(iconUrl != null && iconUrl.length > 0){
                    var fullUrl = baseUriOrigin + resourcesPath + "/" + iconUrl;
                    var logoUrlElem = createElementFromHTML("<img src='" + fullUrl + "' style='max-height:50px; margin: auto;' class='horizontal-padding-10'></img>");
                    logosContainerElem.appendChild(logoUrlElem);
                }
            }
        }

        //set privacy policy url (it's single config entry)
        var privacyPolicyUrl = config['privacyPolicyUrl'];
        var linksContainerElem = document.querySelector('#footer-links-container');
        if(privacyPolicyUrl != null && privacyPolicyUrl.length > 0 && privacyPolicyUrl[0].length > 0){
            var privacyProlicyElem = createElementFromHTML("<a class='horizontal-padding-10' href='" + privacyPolicyUrl[0] + "'>Privacy</a>");
            linksContainerElem.appendChild(privacyProlicyElem);
        }

        //set terms of use policy url (it's single config entry)
        var termsOfUseUrl = config['termsOfUseUrl'];
        var linksContainerElem = document.querySelector('#footer-links-container');
        var defaultTOUUrl = baseUri + "/realms/" + realm + "/theme-info/terms-of-use";
        var termsOfUseElem = createElementFromHTML("<a class='horizontal-padding-10' href='" + defaultTOUUrl + "'>Terms</a>");
        if(termsOfUseUrl != null && termsOfUseUrl.length > 0 && termsOfUseUrl[0].length > 0){
            termsOfUseElem = createElementFromHTML("<a class='horizontal-padding-10' href='" + termsOfUseUrl[0] + "'>Terms</a>");
        }
        linksContainerElem.appendChild(termsOfUseElem);

        //set support url (it's single config entry)
        var supportUrl = config['supportUrl'];
        var supportContainerElem = document.querySelector('#footer-support-container');
        if(supportUrl != null && supportUrl.length > 0 && supportUrl[0].length > 0){
            var supportElem = createElementFromHTML("<a class='horizontal-padding-10' href='" + supportUrl[0] + "'>Support</a>");
            supportContainerElem.appendChild(supportElem);
        }

        //set html footer text (it's single config entry)
        var htmlFooterText = config['htmlFooterText'];
        var footerHtmlTextElem = document.querySelector('#footer-html-text');
        if(htmlFooterText != null && htmlFooterText.length > 0) {
            footerHtmlTextElem.innerHTML = htmlFooterText[0];
        }

        //set a red ribbon if the theme has a ribbon text to show
        var ribbonText = config['ribbonText'];
        if(ribbonText != null && ribbonText.length > 0 && ribbonText[0]) {
            document.body.appendChild(createElementFromHTML("<div class='corner-ribbon'>" + ribbonText + "</div>"));
        }



    }

    function createElementFromHTML(htmlString) {
        var div = document.createElement('div');
        div.innerHTML = htmlString.trim();
        return div.firstChild;
    }


    function drawFooterInPlace(){
        fetch(baseUriOrigin + resourcesPath + "/elements/footer.html")
            .then((r)=>{r.text().then((d)=>{
                let element = createElementFromHTML(d);
                document.getElementsByClassName("login-pf-page")[0].appendChild(element);
                getConfig();
            })
        });
    }

    function removeDefaultLogo() {
        var logoParentDiv = document.querySelector('#kc-header-wrapper');
        for(var i=0 ; i<logoParentDiv.childNodes.length; i++){
            var child = logoParentDiv.childNodes[i];
            if(child.tagName == undefined || child.tagName != 'IMG')
                child.remove();
        }
    }


    document.addEventListener("DOMContentLoaded", function(event) {
      drawFooterInPlace();
      removeDefaultLogo();
    });

  </script>


</body>
</html>
</#macro>
