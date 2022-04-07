/*
 All basic variables which can be used here are loaded by the index.ftl of the parent theme (it's "keycloak.v2" at the time of writing).
 See that file for all the available imported variables
*/



var baseUri = authUrl; //for compatibility with the login/template.ftl
var resourcesPath = resourceUrl; //for compatibility with the login/template.ftl

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
    var logoParentDivs = [document.querySelector('#brandLink'), document.querySelector('#landingLogo')];
    for(let logoParentDiv of logoParentDivs){
        var image = createElementFromHTML("<img src='" + fullUrl + "' alt='" + realm + "' style='height:75px; width:auto;'>")
        logoParentDiv.appendChild(image);
    }

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
            document.body.appendChild(element);
            getConfig();
        })
    });
}

function removeDefaultLogo() {
    var logoParentDivs = [document.querySelector('#brandLink'), document.querySelector('#landingLogo')];
    for(let logoParentDiv of logoParentDivs)
        logoParentDiv.innerHTML="";
}

function executeOverrides(){
    drawFooterInPlace();
    removeDefaultLogo();
}











